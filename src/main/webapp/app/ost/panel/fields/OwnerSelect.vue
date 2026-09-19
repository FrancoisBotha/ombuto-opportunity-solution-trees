<template>
  <div class="ost-field">
    <label class="ost-field__label" :for="id">Owner</label>
    <select :id="id" class="ost-input ost-owner" :value="value" :disabled="readonly" data-cy="ost-owner" @change="onChange">
      <option value="">Unassigned</option>
      <option v-for="option in options" :key="option.login" :value="option.login">{{ option.label }}</option>
    </select>
  </div>
</template>

<script setup lang="ts">
/** Assumption owner: one of the team's members (the server rejects anyone else). */
import { computed } from 'vue';

import type { TeamMemberDTO } from '../../ost.model';
import { memberName } from '../panel-format';

const props = defineProps<{ value: string; members: TeamMemberDTO[]; readonly?: boolean }>();
const emit = defineEmits<{ change: [login: string] }>();

const id = `ost-owner-${Math.random().toString(36).slice(2, 9)}`;

const options = computed(() => {
  const list = props.members.map(m => ({ login: m.login, label: memberName(m) }));
  // An owner who has since left the team still shows (the server keeps the login).
  if (props.value && !list.some(o => o.login === props.value)) list.push({ login: props.value, label: `${props.value} (not a member)` });
  return list;
});

function onChange(event: Event) {
  const login = (event.target as HTMLSelectElement).value;
  if (!props.readonly && login !== props.value) emit('change', login);
}
</script>

<style scoped>
.ost-field .ost-owner {
  min-height: 32px;
  font-size: 13px;
  padding: 4px 8px;
  background: transparent;
}

.ost-owner option {
  background: var(--color-surface);
  color: var(--color-text);
}
</style>
