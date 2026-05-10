package com.claircore.iam.domain.services;

import com.claircore.iam.domain.model.valueobjects.GoogleIdToken;
import com.claircore.iam.domain.model.valueobjects.VerifiedGoogleIdentity;

import java.util.Optional;

public interface GoogleTokenVerifier {
    Optional<VerifiedGoogleIdentity> verify(GoogleIdToken idToken);
}
