package com.opportunity.tree.service.dto;

import java.io.Serializable;

/**
 * Minimal projection of a user for the team member picker: id, login and a display name only —
 * never email or authorities (TEAMS-002 AC 7).
 */
public class UserSearchResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String login;
    private String name;

    public UserSearchResultDTO() {}

    public UserSearchResultDTO(String id, String login, String name) {
        this.id = id;
        this.login = login;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
