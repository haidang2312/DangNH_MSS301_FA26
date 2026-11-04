package com.mss301.brand.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="WatchBrands")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WatchBrand {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="BrandID")
 private Integer brandId;
 @Column(name="BrandName", nullable=false, length=100) private String brandName;
 @Column(name="CountryOfOrigin", nullable=false, length=100) private String countryOfOrigin;
}
