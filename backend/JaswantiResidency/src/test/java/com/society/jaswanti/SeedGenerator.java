package com.society.jaswanti;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.file.Paths;

public class SeedGenerator {
    public static void main(String[] args) throws IOException {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String path = Paths.get("src", "main", "resources", "db", "migration", "V2__seed_rooms_users.sql").toAbsolutePath().toString();
        
        try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
            out.println("-- Seed Users and Rooms (Idempotent using INSERT IGNORE / NOT EXISTS)");
            out.println();
            
            // Need to insert admin first so we have at least one admin
            String adminHashed = encoder.encode("admin123");
            out.println("INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)");
            out.println("SELECT 'admin@jaswanti.com', 1, 1, 'Admin', '" + adminHashed + "', 'ADMIN', 'ACTIVE', 0, NOW()");
            out.println("WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@jaswanti.com');");
            out.println();
            
            for (int floor = 1; floor <= 10; floor++) {
                for (int roomNum = 1; roomNum <= 8; roomNum++) {
                    String roomStr = String.format("C-%d%02d", floor, roomNum);
                    String tempPassword = "Temp_" + roomStr;
                    String hashed = encoder.encode(tempPassword);
                    String email = "resident" + floor + String.format("%02d", roomNum) + "@jaswanti.com";
                    String name = "Resident " + floor + String.format("%02d", roomNum);
                    
                    // User insert
                    out.println("INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, created_at)");
                    out.println("SELECT '" + email + "', 0, 1, '" + name + "', '" + hashed + "', 'RESIDENT', 'ACTIVE', 1, NOW()");
                    out.println("WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = '" + email + "');");
                    
                    // Room insert
                    out.println("INSERT INTO rooms (room_number, floor, wing, resident_id)");
                    out.println("SELECT '" + roomStr + "', " + floor + ", 'C', id");
                    out.println("FROM users WHERE email = '" + email + "'");
                    out.println("AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = '" + roomStr + "');");
                    out.println();
                }
            }
            
            System.out.println("Seed script generated successfully at " + path);
        }
    }
}
