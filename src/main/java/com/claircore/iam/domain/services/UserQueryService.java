package com.claircore.iam.domain.services;

import com.claircore.iam.domain.model.entities.User;
import com.claircore.iam.domain.model.queries.GetUserByEmailQuery;

import java.util.Optional;

public interface UserQueryService {
    Optional<User> handle(GetUserByEmailQuery query);
}
