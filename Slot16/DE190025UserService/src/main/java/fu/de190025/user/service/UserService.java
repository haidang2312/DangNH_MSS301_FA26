package fu.de190025.user.service;
import fu.de190025.user.dto.*;
public interface UserService {
 UserDTO create(UserDTO dto); UserDTO update(Long id,UserDTO dto); UserDTO get(Long id); void deactivate(Long id); PageDTO list(int page,int size,String role,String status);
}