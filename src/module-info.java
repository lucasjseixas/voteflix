module testvoteflix {
    requires java.desktop;
    requires com.google.gson;
    requires com.auth0.jwt;
    exports voteflix.entity;
    exports voteflix.auth;
    exports voteflix.service;
    exports voteflix.dto.request;
    exports voteflix.dto.response;
    exports voteflix.dto;  // ← ADICIONAR
    exports voteflix.repository;
    exports voteflix.util;
}