package com.litsite.lit.controller;

import com.litsite.lit.dto.*;
import com.litsite.lit.models.MyUser;
import com.litsite.lit.service.AuthenticationService;
import com.litsite.lit.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AuthenticationController {
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    @PostMapping("/signup")
    public ResponseEntity<MyUser> register(@RequestBody RegisterUserDto registerUserDto) {
        MyUser registeredUser = authenticationService.signup(registerUserDto);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtAuthDto> authenticate(@RequestBody LoginUserDto loginUserDto){
        MyUser authenticatedUser = authenticationService.authenticate(loginUserDto);
        JwtAuthDto jwtAuthDto = jwtService.generateAuthToken(authenticatedUser.getEmail());
        return ResponseEntity.ok(jwtAuthDto);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserDto verifyUserDto) {
        try {
            authenticationService.verifyUser(verifyUserDto);
            return ResponseEntity.ok("Account verified successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public JwtAuthDto refresh(@RequestBody RefreshTokenDto refreshTokenDto) throws Exception {
        return authenticationService.refreshToken(refreshTokenDto);
    }
}
