package music.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import music.library.entity.Album;

/**
 * Spring Data JPA repository for Album entity.
 * 
 * Provides standard CRUD operations, custom query methods, and dynamic
 * specification-based queries for Album entities. Extends both JpaRepository
 * for basic operations and JpaSpecificationExecutor for advanced filtering.
 * 
 * Custom Query Methods:
 * - findByArtist_ArtistId: Retrieves all albums by a specific artist
 * - findByGenres_GenreId: Retrieves all albums in a specific genre
 * 
 * These methods use Spring Data JPA's property expression syntax to navigate
 * relationships (e.g., "artist.artistId" becomes "Artist_ArtistId").
 * 
 * The JpaSpecificationExecutor interface enables dynamic queries using the
 * Criteria API, allowing complex search operations with multiple optional
 * filters (see AlbumSpecs for specification implementations).
 * 
 * @author JC - Backend Developer Bootcamp Portfolio
 * @see Album
 * @see JpaRepository
 * @see JpaSpecificationExecutor
 * @see music.library.specification.AlbumSpecs
 */
public interface AlbumRepository extends JpaRepository<Album, Long>, JpaSpecificationExecutor<Album> {
    
    /**
     * Finds all albums by a specific artist.
     * Uses Spring Data JPA property expression to navigate the artist relationship.
     * 
     * @param artistId the artist's ID
     * @return list of albums by the artist (empty if none found)
     */
    @EntityGraph(attributePaths = {"artist", "genres"})
    List<Album> findByArtist_ArtistId(Long artistId);
    
    /**
     * Finds all albums that have a specific genre.
     * Uses Spring Data JPA property expression to navigate the many-to-many genres relationship.
     * 
     * @param genreId the genre's ID
     * @return list of albums in the genre (empty if none found)
     */
    @EntityGraph(attributePaths = {"artist", "genres"})
    List<Album> findByGenres_GenreId(Long genreId);
    
    /**
     * Finds an album by title (case-insensitive).
     * Used for duplicate checking before creating new albums.
     * 
     * @param title the album title to search for
     * @return Optional containing the album if found, empty otherwise
     */
    Optional<Album> findByTitleIgnoreCase(String title);
	
	/**
	* Searches albums by title OR artist name (case-insensitive substring match).
	* Returns albums, not artists - so album covers display in the UI.
	* 
	* @param query the search term
	* @param pageable pagination parameters
	* @return paginated albums matching the search
	*/
	@EntityGraph(attributePaths = {"artist", "genres"})
	@Query("SELECT a FROM Album a WHERE " +
		   "LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
		   "LOWER(a.artist.name) LIKE LOWER(CONCAT('%', :query, '%'))")
	Page<Album> searchByTitleOrArtist(@Param("query") String query, Pageable pageable);
    
    /**
     * Checks if an album with the given title exists (case-insensitive).
     * More efficient than findByTitleIgnoreCase when only existence check is needed.
     * 
     * @param title the album title to check
     * @return true if an album with this title exists, false otherwise
     */
    boolean existsByTitleIgnoreCase(String title);
}
