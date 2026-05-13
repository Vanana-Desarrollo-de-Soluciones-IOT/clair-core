Feature: Register a new account
  As a Visitor, I want to create an account with my email and a password, so that I can access Clair as a registered Customer.

  Scenario: Successful registration
    Given the Visitor is on the sign-up page and has no existing account with the submitted email
    When the Visitor submits a valid email, a password meeting the strength policy, and accepts the terms
    Then a Customer account is created in an "unverified" state
    And a verification email is dispatched
    And the Visitor is informed that verification is required to continue

  Scenario: Duplicate email
    Given an account already exists for the submitted email
    When the Visitor attempts to register
    Then registration is rejected with a clear, non-revealing message
