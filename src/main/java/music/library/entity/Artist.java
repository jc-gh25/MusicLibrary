
package music.library.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "artist")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "Music artist entity")

public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier", example = "1")
    private Long artistId;
	
	//Add a getter with the variable that pagination uses
	public Long getId() {
        return artistId;
    }

    @Column(nullable = false, length = 255, unique = true)
    @NotBlank(message = "Artist name must not be blank")
    @Size(max = 255, message = "Artist name must be ≤ 255 characters")
    @Schema(description = "Artist name", example = "The Beatles") 
    private String name;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Artist description or biography", example = "British rock band formed in Liverpool")
    private String description;   // not using validation
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Creation timestamp", example = "2024-12-09T10:30:00") 
    private LocalDateTime createdAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Update timestamp", example = "2024-12-09T10:30:00") 
    private LocalDateTime updatedAt;

    /* One-to-many with Album */
    @JsonIgnore /*This hides the collection from the JSON representation, 
    * breaking the cycle for serialization and for OpenAPI generation.*/
    @OneToMany(mappedBy = "artist",
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @Builder.Default // tell Lombok to keep this init
    @Schema(hidden = true) // This tells SpringDoc to hide it from API docs too
    private Set<Album> albums = new HashSet<>();   // generic type added

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
