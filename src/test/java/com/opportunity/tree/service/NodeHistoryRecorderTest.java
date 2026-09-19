package com.opportunity.tree.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.NodeHistoryRepository;
import com.opportunity.tree.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** Unit tests for {@link NodeHistoryRecorder}. */
@ExtendWith(MockitoExtension.class)
class NodeHistoryRecorderTest {

    @Mock
    private NodeHistoryRepository nodeHistoryRepository;

    @Mock
    private UserRepository userRepository;

    private NodeHistoryRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new NodeHistoryRecorder(nodeHistoryRepository, userRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordSetsAllFieldsAuthorAndTimestamp() {
        authenticate("alice");
        User alice = new User();
        alice.setLogin("alice");
        when(userRepository.findOneByLogin("alice")).thenReturn(Optional.of(alice));
        when(nodeHistoryRepository.save(any(NodeHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        NodeHistory saved = recorder.record(TreeNodeType.OPPORTUNITY, 12L, HistoryEventType.STATUS_CHANGED, "Status → Validated");
        Instant after = Instant.now();

        assertThat(saved.getNodeType()).isEqualTo(TreeNodeType.OPPORTUNITY);
        assertThat(saved.getNodeId()).isEqualTo(12L);
        assertThat(saved.getEventType()).isEqualTo(HistoryEventType.STATUS_CHANGED);
        assertThat(saved.getSummary()).isEqualTo("Status → Validated");
        assertThat(saved.getAuthor()).isSameAs(alice);
        assertThat(saved.getCreatedDate()).isBetween(before, after);
        verify(nodeHistoryRepository).save(saved);
    }

    @Test
    void recordWithoutAuthenticatedUserLeavesAuthorEmpty() {
        when(nodeHistoryRepository.save(any(NodeHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        NodeHistory saved = recorder.record(TreeNodeType.SOLUTION, 3L, HistoryEventType.CREATED, "Created");

        assertThat(saved.getAuthor()).isNull();
        verify(userRepository, never()).findOneByLogin(any());
    }

    @Test
    void recordTruncatesOverlongSummaryAndDefaultsNullToEmpty() {
        when(nodeHistoryRepository.save(any(NodeHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        NodeHistory longOne = recorder.record(TreeNodeType.OUTCOME, 1L, HistoryEventType.MOVED, "x".repeat(600));
        NodeHistory nullOne = recorder.record(TreeNodeType.OUTCOME, 1L, HistoryEventType.MOVED, null);

        assertThat(longOne.getSummary()).hasSize(NodeHistoryRecorder.MAX_SUMMARY_LENGTH).endsWith("…");
        assertThat(nullOne.getSummary()).isEmpty();
    }

    @Test
    void recordRejectsMissingIdentity() {
        assertThatThrownBy(() -> recorder.record(null, 1L, HistoryEventType.CREATED, "x")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> recorder.record(TreeNodeType.PRODUCT, null, HistoryEventType.CREATED, "x")).isInstanceOf(
            NullPointerException.class
        );
        assertThatThrownBy(() -> recorder.record(TreeNodeType.PRODUCT, 1L, null, "x")).isInstanceOf(NullPointerException.class);
        verify(nodeHistoryRepository, never()).save(any());
    }

    @Test
    void priorityBandIsRoundedTenth() {
        assertThat(NodeHistoryRecorder.priorityBand(1)).isZero();
        assertThat(NodeHistoryRecorder.priorityBand(4)).isZero();
        assertThat(NodeHistoryRecorder.priorityBand(5)).isEqualTo(1);
        assertThat(NodeHistoryRecorder.priorityBand(44)).isEqualTo(4);
        assertThat(NodeHistoryRecorder.priorityBand(45)).isEqualTo(5);
        assertThat(NodeHistoryRecorder.priorityBand(100)).isEqualTo(10);
    }

    @Test
    void priorityChangeIsOnlyRecordedWhenTheBandChanges() {
        assertThat(NodeHistoryRecorder.isPriorityBandChange(50, 54)).isFalse();
        assertThat(NodeHistoryRecorder.isPriorityBandChange(45, 54)).isFalse();
        assertThat(NodeHistoryRecorder.isPriorityBandChange(44, 45)).isTrue();
        assertThat(NodeHistoryRecorder.isPriorityBandChange(50, 60)).isTrue();
        assertThat(NodeHistoryRecorder.isPriorityBandChange(60, 50)).isTrue();
        assertThat(NodeHistoryRecorder.isPriorityBandChange(50, 50)).isFalse();
        assertThat(NodeHistoryRecorder.isPriorityBandChange(null, 50)).isTrue();
        assertThat(NodeHistoryRecorder.isPriorityBandChange(null, null)).isFalse();
    }

    private static void authenticate(String login) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(login, "n/a"));
        SecurityContextHolder.setContext(ctx);
    }
}
