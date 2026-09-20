package com.opportunity.tree.service.broadcast;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Remembers which (team, login) pairs have lost read access while a STOMP subscription was open.
 *
 * <p><strong>Why this exists.</strong> The STOMP channel interceptor authorises a SUBSCRIBE
 * once, when the frame arrives. A subscription then lives for as long as the socket does, so a
 * member removed from a team half an hour later keeps receiving that team's tree events — full
 * {@code TreeNodeDTO} payloads, titles and all — on a subscription that was legitimate when it was
 * made and is not any more. Authorisation checked only at entry is exactly the hole FR-034 and
 * NFR-011 are about, so the outbound channel interceptor re-checks every outbound frame against
 * this registry.
 *
 * <p><strong>Why a registry and not a membership query.</strong> The outbound check runs once per
 * subscriber per event, and a cascade delete or a bulk build is a burst of hundreds of events
 * (NFR-014). A database round trip per frame would put the membership table on the hot path of
 * every broadcast. Membership only changes through the paths that publish
 * {@code MEMBERSHIP_CHANGED}, so those paths tell this registry instead: an in-memory set lookup
 * costs nothing per frame.
 *
 * <p>Entries are per application instance, like {@code seq} and {@code epoch} (NFR-012), and are
 * dropped again when the same user is added back to the team. The set therefore holds at most one
 * entry per (team, user) pair that has ever been removed during this instance's uptime.
 */
@Component
public class TreeTopicRevocationRegistry {

    /**
     * Message header naming the login a {@code MEMBERSHIP_CHANGED} removal is about. It is the only
     * way a frame reaches an already-revoked session, so nothing else may set it.
     */
    public static final String REVOKED_LOGIN_HEADER = "ostRevokedLogin";

    private final Set<String> revoked = ConcurrentHashMap.newKeySet();

    /**
     * STOMP session id to the login that subscribed on it. The broker's outbound MESSAGE frames
     * carry the destination and the session id but not always the principal, so the session id is
     * the only reliable way back to a user on the way out. Populated when a SUBSCRIBE is authorised
     * and dropped on DISCONNECT.
     */
    private final Map<String, String> sessionLogins = new ConcurrentHashMap<>();

    private static String key(Long teamId, String login) {
        return teamId + "\u0000" + login;
    }

    /** Stops further team tree frames reaching {@code login}'s open subscriptions on {@code teamId}. */
    public void revoke(Long teamId, String login) {
        if (teamId == null || login == null) return;
        revoked.add(key(teamId, login));
    }

    /** Lets frames flow again — the user is a member of the team once more. */
    public void restore(Long teamId, String login) {
        if (teamId == null || login == null) return;
        revoked.remove(key(teamId, login));
    }

    public boolean isRevoked(Long teamId, String login) {
        return teamId != null && login != null && revoked.contains(key(teamId, login));
    }

    /** Remembers which login a STOMP session belongs to, for the outbound check. */
    public void noteSession(String sessionId, String login) {
        if (sessionId == null || login == null) return;
        sessionLogins.put(sessionId, login);
    }

    /** The login that subscribed on this STOMP session, or {@code null} if it is unknown. */
    public String loginOf(String sessionId) {
        return sessionId == null ? null : sessionLogins.get(sessionId);
    }

    /** Called when a session disconnects, so the map does not grow with the process's uptime. */
    public void forgetSession(String sessionId) {
        if (sessionId != null) sessionLogins.remove(sessionId);
    }

    /** Test seam: forgets every revocation and every tracked session. */
    public void clear() {
        revoked.clear();
        sessionLogins.clear();
    }

    @Override
    public String toString() {
        return "TreeTopicRevocationRegistry" + Objects.toString(revoked);
    }
}
