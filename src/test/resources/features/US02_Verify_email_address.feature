Feature: Verify email address
  As a Customer, I want to confirm ownership of my email through a verification link, so that my account is activated and I can log in.

  Scenario: Valid verification token
    Given the Customer received a verification email with a unique, unexpired token
    When the Customer follows the verification link
    Then the account is marked as verified
    And the Customer is redirected to the login page

  Scenario: Expired or reused token
    Given the verification token is expired or has already been consumed
    When the Customer follows the link
    Then verification fails
    And the Customer is offered to request a new verification email
