package music.library.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "genre")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true) //Avoids accidental recursion; prevents StackOverflowErrors
// if you ever put Genre in a Set that relies on equals().
@Schema(description = "Music genre entity")

public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
	@Schema(description = "Unique identifier", example = "1")
    private Long genreId;

    @Column(nullable = false, length = 100, unique = true)
	@Schema(description = "Genre name", example = "Rock") 
    private String name;

    @Column(columnDefinition = "TEXT")
	@Schema(description = "Genre description", example = "Rock music genre")
    private String description;
	
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	@Schema(description = "Creation timestamp", example = "2024-12-09T10:30:00")
    private LocalDateTime createdAt;
	
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	@Schema(description = "Last update timestamp", example = "2024-12-09T10:30:00")
    private LocalDateTime updatedAt;

    //Inverse side of the many-to-many.
    @JsonIgnore /*This hides the collection from the JSON representation, 
    * breaking the cycle for serialization and for OpenAPI generation.*/
    @ManyToMany(mappedBy = "genres", fetch = FetchType.LAZY)  /* Changed to LAZY to prevent duplicate insert issues 
     * when creating albums. If tests need to access genre.getAlbums(), wrap them in @Transactional
     * or use Hibernate.initialize(genre.getAlbums()) explicitly. */
    
    @Builder.Default  //When the builder is used, start with the value given in the field declaration 
    // unless the call explicitly sets something else.
    private Set<Album> albums = new HashSet<>();

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
