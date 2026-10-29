package fu.de190025.user.dto;
public record PageDTO(int size, int page, int totalPages, long totalElements, boolean first, boolean last, Object content) {
 public static PageDTO of(org.springframework.data.domain.Page<?> p, Object content) { return new PageDTO(p.getSize(),p.getNumber(),p.getTotalPages(),p.getTotalElements(),p.isFirst(),p.isLast(),content); }
}