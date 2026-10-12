package com.sagares.saga_res_api.auth.controller;

import com.sagares.saga_res_api.auth.dto.CurrentUserResponse;
import com.sagares.saga_res_api.auth.dto.LoginRequest;
import com.sagares.saga_res_api.auth.dto.LoginResponse;
import com.sagares.saga_res_api.auth.dto.RegisterRequest;
import com.sagares.saga_res_api.auth.service.AuthService;
import com.sagares.saga_res_api.common.exception.ErrorResponse;
import com.sagares.saga_res_api.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Public customer registration and login, plus authenticated account profile lookup.")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Register a customer account",
            description = """
                    Creates a new account from public registration data. The email is normalized to lowercase and the
                    server always assigns the CUSTOMER role; the client cannot choose a role. Access: anonymous.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer account created", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Malformed JSON or request-field validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "The email is already registered or conflicts with stored data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CurrentUserResponse> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Customer registration details.", required = true,
                    content = @Content(schema = @Schema(implementation = RegisterRequest.class), examples = @ExampleObject(value = """
                            {"email":"customer@example.com","password":"Secret123","fullName":"Nguyen Van A","phone":"0901234567"}
                            """))
            )
            @Valid @RequestBody RegisterRequest request) {
        CurrentUserResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in",
            description = """
                    Authenticates an account using normalized email and password. A successful response contains a JWT
                    access token and basic account information. Missing accounts, wrong passwords, and locked accounts
                    all produce the same unauthorized outcome. Access: anonymous.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Malformed JSON or request-field validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid email or password, or the account is locked",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public LoginResponse login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Account credentials.", required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequest.class), examples = @ExampleObject(value = """
                            {"email":"customer@example.com","password":"Secret123"}
                            """))
            )
            @Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get the current account",
            description = """
                    Loads the current account from the database using the JWT subject and returns its latest profile and
                    role. Missing, invalid, expired, or stale-role tokens and missing or locked accounts are rejected.
                    Access: any authenticated CUSTOMER, STAFF, or ADMIN account.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current account profile", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "Authentication is missing or the access token/account is invalid",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public CurrentUserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.getCurrentUser(user.accountId());
    }
}
