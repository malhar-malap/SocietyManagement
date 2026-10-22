package com.society.jaswanti.security;

import com.society.jaswanti.entity.Room;
import com.society.jaswanti.entity.RoomResident;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.repository.RoomResidentRepository;
import com.society.jaswanti.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomResidentRepository roomResidentRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    RoomRepository roomRepository,
                                    RoomResidentRepository roomResidentRepository) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.roomResidentRepository = roomResidentRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // If identifier has '@', it's an email login attempt
        if (identifier.contains("@")) {
            return userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + identifier));
        } else {
            // Otherwise, treat identifier as Room Number
            Room room = roomRepository.findByRoomNumber(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("Room not found: " + identifier));

            User resident = roomResidentRepository.findFirstByRoomIdAndIsActiveTrueOrderByIsPrimaryContactDescIdDesc(room.getId())
                    .map(RoomResident::getResident)
                    .orElseGet(() -> {
                        if (!roomResidentRepository.existsByRoomId(room.getId())) {
                            return room.getResident();
                        }
                        return null;
                    });

            if (resident == null) {
                throw new UsernameNotFoundException("No active resident is assigned to room: " + identifier);
            }
            if (!resident.isForcePasswordChange()) {
                throw new UsernameNotFoundException("Profile setup is complete. Please log in using your verified email address.");
            }

            return resident;
        }
    }
}
