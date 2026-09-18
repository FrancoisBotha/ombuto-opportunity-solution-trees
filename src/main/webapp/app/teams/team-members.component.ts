import { computed, defineComponent, inject, onMounted, ref, watch } from 'vue';

import { useAlertService } from '@/shared/alert/alert.service';
import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import type { IAddTeamMemberRequest, IChangeTeamMemberRoleRequest, ITeamMember, IUserSearchResult } from './my-team.model';
import TeamsService from './teams.service';
import { useTeamsStore } from './teams.store';

const LAST_OWNER_HINT = 'A team needs at least one owner.';
const LAST_OWNER_ERROR = 'A team needs at least one owner — you cannot remove or demote the only owner.';

export default defineComponent({
  name: 'TeamMembers',
  props: {
    teamId: {
      type: Number,
      required: true,
    },
  },
  setup(props) {
    const teamsService = inject('teamsService', () => new TeamsService(), true);
    const alertService = inject('alertService', () => useAlertService(), true);
    const teamsStore = useTeamsStore();

    const members = ref<ITeamMember[]>([]);
    const loading = ref(false);
    const listError = ref<string | null>(null);
    const actionError = ref<string | null>(null);
    const busy = ref(false);

    const showAddForm = ref(false);
    const searchQuery = ref('');
    const searchResults = ref<IUserSearchResult[]>([]);
    const searchLoading = ref(false);
    const selectedUser = ref<IUserSearchResult | null>(null);
    const selectedRole = ref<TeamRole>(TeamRole.VIEWER);
    const addError = ref<string | null>(null);

    const removeConfirm = ref<ITeamMember | null>(null);

    const isOwner = computed(() => teamsStore.isOwner(props.teamId));
    const ownerCount = computed(() => members.value.filter(m => m.role === TeamRole.OWNER).length);
    const roleOptions: TeamRole[] = [TeamRole.OWNER, TeamRole.EDITOR, TeamRole.VIEWER];

    const isLastOwner = (member: ITeamMember): boolean => member.role === TeamRole.OWNER && ownerCount.value <= 1;

    const displayName = (m: ITeamMember): string => {
      const parts = [m.firstName, m.lastName].filter(p => p && p.trim().length > 0);
      return parts.length ? parts.join(' ') : m.login;
    };

    const translateError = (err: any, fallback = 'Something went wrong'): string => {
      const status = err?.response?.status;
      const data = err?.response?.data ?? {};
      const key: string | undefined = data.errorKey ?? data.message;
      if (key === 'lastowner' || (typeof data.detail === 'string' && data.detail.includes('lastowner'))) {
        return LAST_OWNER_ERROR;
      }
      if (key === 'memberexists') {
        return 'That user is already a member of this team.';
      }
      if (key === 'usernotfound' || key === 'usernotsynced') {
        return 'That user has not signed in yet, so they cannot be added.';
      }
      if (status === 403) {
        return 'You do not have permission to perform this action.';
      }
      return data.detail ?? data.title ?? fallback;
    };

    const loadMembers = async () => {
      loading.value = true;
      listError.value = null;
      try {
        members.value = await teamsService().listMembers(props.teamId);
      } catch (err: any) {
        listError.value = 'Unable to load members';
        alertService.showHttpError(err.response ?? { status: 0, data: {} });
      } finally {
        loading.value = false;
      }
    };

    const openAddForm = () => {
      searchQuery.value = '';
      searchResults.value = [];
      selectedUser.value = null;
      selectedRole.value = TeamRole.VIEWER;
      addError.value = null;
      showAddForm.value = true;
    };

    const cancelAdd = () => {
      showAddForm.value = false;
      addError.value = null;
    };

    const doSearch = async () => {
      searchLoading.value = true;
      try {
        searchResults.value = await teamsService().searchUsers(props.teamId, searchQuery.value);
      } catch (err: any) {
        alertService.showHttpError(err.response ?? { status: 0, data: {} });
      } finally {
        searchLoading.value = false;
      }
    };

    const selectUser = (user: IUserSearchResult) => {
      selectedUser.value = user;
    };

    const submitAdd = async () => {
      if (!selectedUser.value) {
        addError.value = 'Choose a user first';
        return;
      }
      const request: IAddTeamMemberRequest = {
        userId: selectedUser.value.id,
        role: selectedRole.value,
      };
      busy.value = true;
      addError.value = null;
      try {
        await teamsService().addMember(props.teamId, request);
        alertService.showSuccess('Member added');
        showAddForm.value = false;
        await loadMembers();
      } catch (err: any) {
        addError.value = translateError(err, 'Could not add member');
      } finally {
        busy.value = false;
      }
    };

    const changeRole = async (member: ITeamMember, newRole: TeamRole) => {
      if (member.role === newRole) return;
      if (isLastOwner(member) && newRole !== TeamRole.OWNER) {
        actionError.value = LAST_OWNER_ERROR;
        return;
      }
      const request: IChangeTeamMemberRoleRequest = { role: newRole };
      busy.value = true;
      actionError.value = null;
      try {
        const updated = await teamsService().changeMemberRole(props.teamId, member.userId, request);
        const idx = members.value.findIndex(m => m.userId === member.userId);
        if (idx >= 0) {
          members.value.splice(idx, 1, updated);
        }
        alertService.showSuccess('Role updated');
      } catch (err: any) {
        actionError.value = translateError(err, 'Could not change role');
        // reload to resync roles the server rejected
        await loadMembers();
      } finally {
        busy.value = false;
      }
    };

    const onRoleSelectChange = (member: ITeamMember, event: Event) => {
      const target = event.target as HTMLSelectElement | null;
      if (!target) return;
      const nextRole = target.value as TeamRole;
      // Revert visual to stored value; changeRole will re-render authoritative state
      target.value = member.role;
      changeRole(member, nextRole);
    };

    const askRemove = (member: ITeamMember) => {
      actionError.value = null;
      removeConfirm.value = member;
    };

    const cancelRemove = () => {
      removeConfirm.value = null;
    };

    const confirmRemove = async () => {
      const member = removeConfirm.value;
      if (!member) return;
      busy.value = true;
      actionError.value = null;
      try {
        await teamsService().removeMember(props.teamId, member.userId);
        alertService.showSuccess('Member removed');
        removeConfirm.value = null;
        await loadMembers();
      } catch (err: any) {
        actionError.value = translateError(err, 'Could not remove member');
      } finally {
        busy.value = false;
      }
    };

    onMounted(loadMembers);
    watch(
      () => props.teamId,
      () => loadMembers(),
    );

    return {
      TeamRole,
      LAST_OWNER_HINT,
      roleOptions,
      members,
      loading,
      listError,
      actionError,
      busy,
      isOwner,
      isLastOwner,
      displayName,
      showAddForm,
      searchQuery,
      searchResults,
      searchLoading,
      selectedUser,
      selectedRole,
      addError,
      removeConfirm,
      loadMembers,
      openAddForm,
      cancelAdd,
      doSearch,
      selectUser,
      submitAdd,
      changeRole,
      onRoleSelectChange,
      askRemove,
      cancelRemove,
      confirmRemove,
    };
  },
});
