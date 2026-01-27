package music.library.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import music.library.dto.CreateAlbumRequest;
import music.library.entity.Album;
import music.library.entity.Artist;
import music.library.entity.Genre;
import music.library.repository.AlbumRepository;
import music.library.repository.ArtistRepository;
import music.library.repository.GenreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AlbumControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;
    private Artist testArtist;
    private Genre testGenre;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/albums";
        
        // Create and save test artist
        testArtist = new Artist();
        testArtist.setName("Test Artist");
        testArtist.setDescription("Test Description");
        testArtist = artistRepository.save(testArtist);
        
        // Create and save test genre
        testGenre = new Genre();
        testGenre.setName("Test Genre");
        testGenre.setDescription("Test Description");
        testGenre = genreRepository.save(testGenre);
    }

    @Test
    void testCreateAlbum() {
        // Create album request DTO to avoid detached entity issues
        CreateAlbumRequest albumRequest = new CreateAlbumRequest();
        albumRequest.setTitle("Test Album");
        albumRequest.setReleaseDate(LocalDate.of(2023, 1, 1));
        albumRequest.setArtistId(testArtist.getArtistId());
        albumRequest.setGenreIds(List.of(testGenre.getGenreId()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateAlbumRequest> request = new HttpEntity<>(albumRequest, headers);

        ResponseEntity<Album> response = restTemplate.postForEntity(baseUrl, request, Album.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Test Album");
        assertThat(response.getBody().getAlbumId()).isNotNull();
    }

    @Test
    void testGetAllAlbums() {
        // Create test album using repository to ensure proper entity management
        Album album = new Album();
        album.setTitle("Test Album");
        album.setReleaseDate(LocalDate.of(2023, 1, 1));
        album.setArtist(testArtist);
        // Use managed genre entity to avoid detached entity issues
        Genre managedGenre = genreRepository.findById(testGenre.getGenreId()).orElseThrow();
        album.setGenres(Set.of(managedGenre));
        albumRepository.save(album);

        ResponseEntity<RestResponsePage<Album>> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<RestResponsePage<Album>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getTitle()).isEqualTo("Test Album");
    }

    @Test
    void testGetAlbumById() {
        // Create test album using repository
        Album album = new Album();
        album.setTitle("Test Album");
        album.setReleaseDate(LocalDate.of(2023, 1, 1));
        album.setArtist(testArtist);
        // Use managed genre entity to avoid detached entity issues
        Genre managedGenre = genreRepository.findById(testGenre.getGenreId()).orElseThrow();
        album.setGenres(Set.of(managedGenre));
        Album savedAlbum = albumRepository.save(album);

        ResponseEntity<Album> response = restTemplate.getForEntity(
                baseUrl + "/" + savedAlbum.getAlbumId(),
                Album.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Test Album");
        assertThat(response.getBody().getAlbumId()).isEqualTo(savedAlbum.getAlbumId());
    }

    @Test
    void testGetAlbumByIdNotFound() {
        ResponseEntity<Album> response = restTemplate.getForEntity(
                baseUrl + "/999",
                Album.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
	
	@Test
	void testSearchAlbumsByTitle() {
		// Create test albums
		Album album1 = new Album();
		album1.setTitle("Abbey Road");
		album1.setReleaseDate(LocalDate.of(1969, 9, 26));
		album1.setArtist(testArtist);
		albumRepository.save(album1);

		Album album2 = new Album();
		album2.setTitle("Let It Be");
		album2.setReleaseDate(LocalDate.of(1970, 5, 8));
		album2.setArtist(testArtist);
		albumRepository.save(album2);

		// Search for "abbey" - should find Abbey Road
		ResponseEntity<RestResponsePage<Album>> response = restTemplate.exchange(
				baseUrl + "/search?q=abbey",
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<RestResponsePage<Album>>() {}
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getContent()).hasSize(1);
		assertThat(response.getBody().getContent().get(0).getTitle()).isEqualTo("Abbey Road");
	}

	@Test
	void testSearchAlbumsByArtistName() {
		// Create a second artist
		Artist beatles = new Artist();
		beatles.setName("The Beatles");
		beatles.setDescription("British rock band");
		beatles = artistRepository.save(beatles);

		// Create album for Beatles
		Album album = new Album();
		album.setTitle("Help!");
		album.setReleaseDate(LocalDate.of(1965, 8, 6));
		album.setArtist(beatles);
		albumRepository.save(album);

		// Search for "beatles" - should find the album by artist name
		ResponseEntity<RestResponsePage<Album>> response = restTemplate.exchange(
				baseUrl + "/search?q=beatles",
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<RestResponsePage<Album>>() {}
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getContent()).hasSize(1);
		assertThat(response.getBody().getContent().get(0).getTitle()).isEqualTo("Help!");
	}

	@Test
	void testSearchAlbumsNoResults() {
		// Search for something that doesn't exist
		ResponseEntity<RestResponsePage<Album>> response = restTemplate.exchange(
				baseUrl + "/search?q=xyznonexistent",
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<RestResponsePage<Album>>() {}
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getContent()).isEmpty();
	}
}