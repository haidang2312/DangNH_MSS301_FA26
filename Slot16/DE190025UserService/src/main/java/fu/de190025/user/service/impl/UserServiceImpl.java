package fu.de190025.user.service.impl;
import fu.de190025.user.entity.*;
import fu.de190025.user.dto.*;
import fu.de190025.user.repository.UserRepository;
import fu.de190025.user.service.UserService;
import fu.de190025.user.common.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional
public class UserServiceImpl implements UserService {
 private final UserRepository repo;
 public UserServiceImpl(UserRepository repo) { this.repo=repo; }
 private User find(Long id) { return repo.findById(id).orElseThrow(()->new ApiException(4,"User not found")); }
 public UserDTO create(UserDTO d) { User u=new User(); apply(u,d); u.setStatus(UserStatus.ACTIVE); validate(u); duplicate(u); return dto(repo.saveAndFlush(u)); }
 public UserDTO update(Long id,UserDTO d) { User u=find(id); apply(u,d); validate(u); duplicate(u); return dto(repo.saveAndFlush(u)); }
 public UserDTO get(Long id) { return dto(find(id)); }
 public void deactivate(Long id) { find(id).setStatus(UserStatus.DEACTIVATED); }
 public PageDTO list(int page,int size,String role,String status) {
  var pageable=Validation.page(page,size);
  Role r=role==null?null:Role.valueOf(role); UserStatus s=status==null?null:UserStatus.valueOf(status);
  var result=repo.findAll((root,q,cb)-> { var p=new java.util.ArrayList<jakarta.persistence.criteria.Predicate>(); if(r!=null)p.add(cb.equal(root.get("role"),r)); if(s!=null)p.add(cb.equal(root.get("status"),s)); return cb.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new)); },pageable);
  return PageDTO.of(result,result.getContent().stream().map(this::dto).toList());
 }
 private void duplicate(User u) { if(repo.existsByUsernameAndUserIdNot(u.getUsername(),u.getUserId()==null?0L:u.getUserId())) throw new ApiException(3,"Duplicate username"); }
 private void validate(User u) { Validation.text(u.getUsername(),30,"username"); Validation.email(u.getEmail()); Validation.require(u.getRole()!=null,"role is required"); Validation.require(u.getRatePerPost()!=null && u.getRatePerPost().signum()>0 && u.getRatePerPost().scale()<=2 && u.getRatePerPost().precision()-u.getRatePerPost().scale()<=16,"Invalid ratePerPost"); Validation.require(u.getMaxDailyPosts()!=null && u.getMaxDailyPosts()>=1 && u.getMaxDailyPosts()<=10,"Invalid maxDailyPosts"); Validation.require(u.getLevel()!=null && u.getLevel()>=1,"Invalid level"); }
 private void apply(User u,UserDTO d) {
 if(d.getUsername()!=null) u.setUsername(d.getUsername());
 if(d.getEmail()!=null) u.setEmail(d.getEmail());
 if(d.getRole()!=null) u.setRole(Role.valueOf(d.getRole()));
 if(d.getRatePerPost()!=null) u.setRatePerPost(d.getRatePerPost());
 if(d.getMaxDailyPosts()!=null) u.setMaxDailyPosts(d.getMaxDailyPosts());
 if(d.getLevel()!=null) u.setLevel(d.getLevel());
 if(d.getStatus()!=null) u.setStatus(UserStatus.valueOf(d.getStatus()));

 }
 private UserDTO dto(User u) { UserDTO d=new UserDTO();
 d.setUserId(u.getUserId());
 d.setUsername(u.getUsername());
 d.setEmail(u.getEmail());
 d.setRole(u.getRole().name());
 d.setRatePerPost(u.getRatePerPost());
 d.setMaxDailyPosts(u.getMaxDailyPosts());
 d.setLevel(u.getLevel());
 d.setStatus(u.getStatus().name());
return d; }
}
