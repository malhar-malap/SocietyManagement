package com.society.jaswanti.security;

import com.society.jaswanti.entity.Room;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    public CustomUserDetailsService(UserRepository userRepository, RoomRepository roomRepository) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // If identifier has '@', it's an email login attempt
        if (identifier.contains("@")) {
            User user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + identifier));
            
            // Can add check if email is verified here if required, or handle in auth provider.
            return user;
        } else {
            // Otherwise, treat identifier as Room Number
            Room room = roomRepository.findByRoomNumber(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("Room not found: " + identifier));
            
            User resident = room.getResident();
            if (resident == null) {
                throw new UsernameNotFoundException("No resident is assigned to room: " + identifier);
            }
            if (!resident.isForcePasswordChange()) {
                throw new UsernameNotFoundException("Profile setup is complete. Please log in using your verified email address.");
            }
            
            return resident;
        }
    }
}
