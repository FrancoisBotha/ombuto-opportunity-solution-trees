<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="teamMember">
        <h2 class="jh-entity-heading" data-cy="teamMemberDetailsHeading"><span>Team MemberTeamMember</span> {{ teamMember.id }}</h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Role</span>
          </dt>
          <dd>
            <span>{{ teamMember.role }}</span>
          </dd>
          <dt>
            <span>Joined Date</span>
          </dt>
          <dd>
            <span v-if="teamMember.joinedDate">{{ formatDateLong(teamMember.joinedDate) }}</span>
          </dd>
          <dt>
            <span>Team</span>
          </dt>
          <dd>
            <div v-if="teamMember.team">
              <router-link :to="{ name: 'TeamView', params: { teamId: teamMember.team.id } }">{{ teamMember.team.name }}</router-link>
            </div>
          </dd>
          <dt>
            <span>User</span>
          </dt>
          <dd>
            {{ teamMember.user ? teamMember.user.login : '' }}
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link
          v-if="teamMember.id"
          :to="{ name: 'TeamMemberEdit', params: { teamMemberId: teamMember.id } }"
          custom
          v-slot="{ navigate }"
        >
          <button @click="navigate" class="btn btn-primary">
            <font-awesome-icon icon="pencil-alt"></font-awesome-icon>&nbsp;<span>Edit</span>
          </button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./team-member-details.component.ts"></script>
