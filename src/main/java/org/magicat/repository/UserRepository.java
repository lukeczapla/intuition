package org.magicat.repository;

import org.jetbrains.annotations.NotNull;
import org.magicat.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    User findByEmailAddress(String emailAddress);

    @Override
    <S extends User> List<S> saveAll(@NotNull Iterable<S> entities);

    @NotNull
    @Override
    <S extends User> S insert(@NotNull S entity);

    @NotNull
    @Override
    <S extends User> List<S> insert(@NotNull Iterable<S> entities);

    @NotNull
    @Override
    <S extends User> S save(@NotNull S entity);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteById(@NotNull String id);

    @Secured("ROLE_ADMIN")
    @Override
    void delete(@NotNull User user);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteAll(Iterable<? extends User> users);

    @Secured("ROLE_ADMIN")
    @Override
    void deleteAll();

}
