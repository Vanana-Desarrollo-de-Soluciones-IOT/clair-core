Feature: Log in
  As a Customer, I want to authenticate with my credentials, so that I can access my personalized Clair workspace.

  Scenario: Successful login
    Given the Customer's account is verified and the credentials match
    When the Customer submits email and password
    Then a session is established
    And the Customer is routed to their dashboard

  Scenario: Invalid credentials
    Given the submitted credentials do not match any verified account
    When the Customer attempts to log in
    Then access is denied with a generic error that does not reveal which field is wrong

  Scenario: Unverified account
    Given the account exists but has not been verified
    When the Customer attempts to log in
    Then access is denied
    And the Customer is offered to resend the verification email
