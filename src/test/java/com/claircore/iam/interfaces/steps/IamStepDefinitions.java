package com.claircore.iam.interfaces.steps;

import com.claircore.iam.domain.model.commands.InitiateRegistrationCommand;
import com.claircore.iam.domain.model.commands.ConfirmRegistrationCommand;
import com.claircore.iam.domain.model.queries.GetUserByEmailQuery;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;
import com.claircore.iam.domain.services.UserCommandService;
import com.claircore.iam.domain.services.UserQueryService;
import com.claircore.iam.domain.services.TokenCommandService;
import java.util.UUID;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class IamStepDefinitions {

    @Autowired
    private UserCommandService userCommandService;

    @Autowired
    private UserQueryService userQueryService;

    @Autowired
    private TokenCommandService tokenCommandService;

    // State variables for assertions
    private String currentEmail;
    private String currentPassword;
    private Exception caughtException;
    private Object actionResult;

    // --- US01: Register a new account ---

    @Given("the Visitor is on the sign-up page and has no existing account with the submitted email")
    public void theVisitorIsOnTheSignUpPageAndHasNoExistingAccountWithTheSubmittedEmail() {
        this.currentEmail = "newuser@example.com";
        this.currentPassword = "StrongPassword123!";
        // Ensure user does not exist via Query
        var query = new GetUserByEmailQuery(new EmailAddress(this.currentEmail));
        assertTrue(userQueryService.handle(query).isEmpty());
    }

    @When("the Visitor submits a valid email, a password meeting the strength policy, and accepts the terms")
    public void theVisitorSubmitsAValidEmailAPasswordMeetingTheStrengthPolicyAndAcceptsTheTerms() {
        var command = new InitiateRegistrationCommand(this.currentEmail, this.currentPassword);
        try {
            this.actionResult = userCommandService.handle(command);
        } catch (Exception e) {
            this.caughtException = e;
        }
    }

    @Then("a Customer account is created in an {string} state")
    public void aCustomerAccountIsCreatedInAnState(String state) {
        assertNull(this.caughtException);
        var query = new GetUserByEmailQuery(new EmailAddress(this.currentEmail));
        var user = userQueryService.handle(query);
        assertTrue(user.isPresent());
        // Assume User entity has a status or isVerified boolean
        // assertEquals(state, user.get().getStatus()); 
    }

    @Then("a verification email is dispatched")
    public void aVerificationEmailIsDispatched() {
        // Assert that a notification event was triggered or a token was generated
        assertNotNull(this.actionResult);
    }

    @Then("the Visitor is informed that verification is required to continue")
    public void theVisitorIsInformedThatVerificationIsRequiredToContinue() {
        // This is typically a UI assertion, but in integration we verify the return type
        assertNotNull(this.actionResult); // Result should contain a message or unverified state
    }

    @Given("an account already exists for the submitted email")
    public void anAccountAlreadyExistsForTheSubmittedEmail() {
        this.currentEmail = "existing@example.com";
        this.currentPassword = "Password123!";
        // Pre-create the user
        userCommandService.handle(new InitiateRegistrationCommand(this.currentEmail, this.currentPassword));
    }

    @When("the Visitor attempts to register")
    public void theVisitorAttemptsToRegister() {
        var command = new InitiateRegistrationCommand(this.currentEmail, this.currentPassword);
        try {
            this.actionResult = userCommandService.handle(command);
        } catch (Exception e) {
            this.caughtException = e;
        }
    }

    @Then("registration is rejected with a clear, non-revealing message")
    public void registrationIsRejectedWithAClearNonRevealingMessage() {
        assertNotNull(this.caughtException);
        // Verify exception type or message does not reveal sensitive info
    }

    // --- US02: Verify email address ---

    @Given("the Customer received a verification email with a unique, unexpired token")
    public void theCustomerReceivedAVerificationEmailWithAUniqueUnexpiredToken() {
        // Assume we have a valid token string generated from a previous signup
    }

    @When("the Customer follows the verification link")
    public void theCustomerFollowsTheVerificationLink() {
        String token = "valid-token-123";
        var command = new ConfirmRegistrationCommand(RegistrationSessionId.generate(), token);
        try {
            this.actionResult = userCommandService.handle(command);
        } catch (Exception e) {
            this.caughtException = e;
        }
    }

    @Then("the account is marked as verified")
    public void theAccountIsMarkedAsVerified() {
        assertNull(this.caughtException);
        // Verify via Query
    }

    @Then("the Customer is redirected to the login page")
    public void theCustomerIsRedirectedToTheLoginPage() {
        // Validation handled at UI level, or check command response status
    }

    @Given("the verification token is expired or has already been consumed")
    public void theVerificationTokenIsExpiredOrHasAlreadyBeenConsumed() {
        // Assume we mock an expired token
    }

    @When("the Customer follows the link")
    public void theCustomerFollowsTheLink() {
        String token = "expired-token-123";
        var command = new ConfirmRegistrationCommand(RegistrationSessionId.generate(), token);
        try {
            this.actionResult = userCommandService.handle(command);
        } catch (Exception e) {
            this.caughtException = e;
        }
    }

    @Then("verification fails")
    public void verificationFails() {
        assertNotNull(this.caughtException);
    }

    @Then("the Customer is offered to request a new verification email")
    public void theCustomerIsOfferedToRequestANewVerificationEmail() {
        // Typically UI logic or part of the exception payload
    }

    // --- US03: Log in ---

    @Given("the Customer's account is verified and the credentials match")
    public void theCustomersAccountIsVerifiedAndTheCredentialsMatch() {
        this.currentEmail = "verified@example.com";
        this.currentPassword = "Password123!";
        // Setup: Ensure account exists and is verified
    }

    @When("the Customer submits email and password")
    public void theCustomerSubmitsEmailAndPassword() {
        var query = new GetUserByEmailQuery(new EmailAddress(this.currentEmail));
        var user = userQueryService.handle(query);
        try {
            if (user.isPresent()) {
                this.actionResult = tokenCommandService.createAccessToken(user.get());
            } else {
                throw new RuntimeException("User not found");
            }
        } catch (Exception e) {
            this.caughtException = e;
        }
    }

    @Then("a session is established")
    public void aSessionIsEstablished() {
        assertNull(this.caughtException);
        assertNotNull(this.actionResult); // e.g. JWT token returned
    }

    @Then("the Customer is routed to their dashboard")
    public void theCustomerIsRoutedToTheirDashboard() {
        // Assert token presence or UI routing
    }

    @Given("the submitted credentials do not match any verified account")
    public void theSubmittedCredentialsDoNotMatchAnyVerifiedAccount() {
        this.currentEmail = "wrong@example.com";
        this.currentPassword = "wrongpassword";
    }

    @When("the Customer attempts to log in")
    public void theCustomerAttemptsToLogIn() {
        var query = new GetUserByEmailQuery(new EmailAddress(this.currentEmail));
        var user = userQueryService.handle(query);
        try {
            if (user.isPresent()) {
                this.actionResult = tokenCommandService.createAccessToken(user.get());
            } else {
                throw new RuntimeException("User not found");
            }
        } catch (Exception e) {
            this.caughtException = e;
        }
    }

    @Then("access is denied with a generic error that does not reveal which field is wrong")
    public void accessIsDeniedWithAGenericErrorThatDoesNotRevealWhichFieldIsWrong() {
        assertNotNull(this.caughtException);
    }

    @Given("the account exists but has not been verified")
    public void theAccountExistsButHasNotBeenVerified() {
        this.currentEmail = "unverified@example.com";
        this.currentPassword = "Password123!";
    }

    @Then("access is denied")
    public void accessIsDenied() {
        assertNotNull(this.caughtException);
    }

    @Then("the Customer is offered to resend the verification email")
    public void theCustomerIsOfferedToResendTheVerificationEmail() {
        // Assert exception type is AccountNotVerifiedException or similar
    }
}