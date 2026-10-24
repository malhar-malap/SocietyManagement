package com.society.jaswanti.service;

import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.entity.RoomResident;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.exception.InvalidFileException;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.repository.RoomResidentRepository;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FileUploadValidationTests {

    @Autowired
    private FileValidationService fileValidationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomResidentRepository roomResidentRepository;

    @Autowired
    private JwtService jwtService;

    private User residentUser;
    private Room testRoom;
    private String residentToken;

    @BeforeEach
    void setUp() {
        residentUser = userRepository.findByEmailIgnoreCase("upload-test-resident@jaswanti.com")
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail("upload-test-resident@jaswanti.com");
                    u.setPassword("hashedpassword123");
                    u.setName("Upload Test Resident");
                    u.setRole(Role.RESIDENT);
                    u.setStatus(UserStatus.ACTIVE);
                    u.setActive(true);
                    u.setEmailVerified(true);
                    u.setForcePasswordChange(false);
                    return userRepository.save(u);
                });

        testRoom = roomRepository.findByRoomNumber("TEST-701")
                .orElseGet(() -> {
                    Room r = new Room();
                    r.setRoomNumber("TEST-701");
                    r.setWing("C");
                    r.setFloor(7);
                    return roomRepository.save(r);
                });

        if (roomResidentRepository.findByRoomIdAndResidentIdAndIsActiveTrue(testRoom.getId(), residentUser.getId()).isEmpty()) {
            RoomResident rr = new RoomResident();
            rr.setRoom(testRoom);
            rr.setResident(residentUser);
            rr.setActive(true);
            rr.setPrimaryContact(true);
            roomResidentRepository.save(rr);
        }

        residentToken = jwtService.generateToken(residentUser);
    }

    @Test
    @DisplayName("Valid JPEG file is accepted and detected as image/jpeg")
    void validJpegFilePasses() {
        byte[] jpegBytes = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F' };
        MockMultipartFile file = new MockMultipartFile("screenshot", "photo.jpg", "image/jpeg", jpegBytes);

        String mime = fileValidationService.validateImage(file);
        assertThat(mime).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("Valid PNG file is accepted and detected as image/png")
    void validPngFilePasses() {
        byte[] pngBytes = new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13 };
        MockMultipartFile file = new MockMultipartFile("screenshot", "photo.png", "image/png", pngBytes);

        String mime = fileValidationService.validateImage(file);
        assertThat(mime).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Valid WEBP file is accepted and detected as image/webp")
    void validWebpFilePasses() {
        byte[] webpBytes = new byte[] { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0, 0 };
        MockMultipartFile file = new MockMultipartFile("screenshot", "photo.webp", "image/webp", webpBytes);

        String mime = fileValidationService.validateImage(file);
        assertThat(mime).isEqualTo("image/webp");
    }

    @Test
    @DisplayName("Valid PDF file is accepted when allowPdf is true")
    void validPdfFilePassesWhenAllowed() {
        byte[] pdfBytes = "%PDF-1.4 test content".getBytes();
        MockMultipartFile file = new MockMultipartFile("doc", "doc.pdf", "application/pdf", pdfBytes);

        String mime = fileValidationService.validateImageOrPdf(file);
        assertThat(mime).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("PDF file is rejected when only images are permitted")
    void pdfRejectedWhenOnlyImagesAllowed() {
        byte[] pdfBytes = "%PDF-1.4 test content".getBytes();
        MockMultipartFile file = new MockMultipartFile("img", "doc.pdf", "application/pdf", pdfBytes);

        assertThatThrownBy(() -> fileValidationService.validateImage(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("PDF files are not permitted");
    }

    @Test
    @DisplayName("Spoofed .exe file renamed to .png is rejected by magic byte inspection")
    void spoofedExeRenamedToPngIsRejected() {
        // MZ header: Windows executable
        byte[] exeBytes = new byte[] { 'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0 };
        MockMultipartFile spoofedFile = new MockMultipartFile("screenshot", "malware.png", "image/png", exeBytes);

        assertThatThrownBy(() -> fileValidationService.validateImageOrPdf(spoofedFile))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Invalid or unrecognized file format");
    }

    @Test
    @DisplayName("Spoofed .html file with image content-type is rejected")
    void spoofedHtmlRenamedToJpegIsRejected() {
        byte[] htmlBytes = "<html><script>alert(1)</script></html>".getBytes();
        MockMultipartFile spoofedFile = new MockMultipartFile("screenshot", "photo.jpg", "image/jpeg", htmlBytes);

        assertThatThrownBy(() -> fileValidationService.validateImageOrPdf(spoofedFile))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Invalid or unrecognized file format");
    }

    @Test
    @DisplayName("Empty file is rejected")
    void emptyFileIsRejected() {
        MockMultipartFile emptyFile = new MockMultipartFile("screenshot", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> fileValidationService.validateImageOrPdf(emptyFile))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    @DisplayName("Oversize file exceeding property limit is rejected")
    void oversizeFileIsRejected() {
        FileValidationService smallLimitService = new FileValidationService(1024); // 1KB limit
        byte[] largeBytes = new byte[2048];
        largeBytes[0] = (byte) 0xFF;
        largeBytes[1] = (byte) 0xD8;
        largeBytes[2] = (byte) 0xFF;

        MockMultipartFile largeFile = new MockMultipartFile("screenshot", "large.jpg", "image/jpeg", largeBytes);

        assertThatThrownBy(() -> smallLimitService.validateImage(largeFile))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("exceeds maximum allowed limit");
    }

    @Test
    @DisplayName("HTTP POST with spoofed file returns 400 Bad Request via GlobalExceptionHandler")
    void httpUploadSpoofedFileReturns400() throws Exception {
        byte[] fakeExe = new byte[] { 'M', 'Z', 1, 2, 3, 4, 5, 6 };
        MockMultipartFile file = new MockMultipartFile("screenshot", "proof.png", "image/png", fakeExe);

        mockMvc.perform(multipart("/api/payments/submissions")
                        .file(file)
                        .param("roomId", testRoom.getId().toString())
                        .param("amount", "2500.00")
                        .param("paymentMode", "UPI")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid or unrecognized file format")));
    }
}
