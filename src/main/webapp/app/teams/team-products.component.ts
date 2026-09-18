import { computed, defineComponent, inject, onMounted, ref, watch } from 'vue';

import { useAlertService } from '@/shared/alert/alert.service';

import type { ICreateTeamProductRequest, ITeamProduct, IUpdateTeamProductRequest } from './my-team.model';
import TeamsService from './teams.service';
import { useTeamsStore } from './teams.store';

export default defineComponent({
  name: 'TeamProducts',
  props: {
    teamId: {
      type: Number,
      required: true,
    },
  },
  setup(props) {
    const teamsService = inject('teamsService', () => new TeamsService());
    const alertService = inject('alertService', () => useAlertService(), true);
    const teamsStore = useTeamsStore();

    const products = ref<ITeamProduct[]>([]);
    const loading = ref(false);
    const listError = ref<string | null>(null);
    const actionError = ref<string | null>(null);
    const busy = ref(false);

    const showCreateForm = ref(false);
    const newProductName = ref('');
    const newProductDescription = ref('');
    const createError = ref<string | null>(null);

    const editingId = ref<number | null>(null);
    const editName = ref('');
    const editDescription = ref('');
    const editError = ref<string | null>(null);

    const canEdit = computed(() => teamsStore.canEdit(props.teamId));

    const translateError = (err: any, fallback = 'Something went wrong'): string => {
      const status = err?.response?.status;
      const data = err?.response?.data ?? {};
      if (status === 403) {
        return 'You do not have permission to perform this action.';
      }
      if (status === 400 || status === 422) {
        return data.detail ?? data.title ?? data.message ?? 'The request was rejected by the server.';
      }
      return data.detail ?? data.title ?? fallback;
    };

    const syncProductCount = (count: number) => {
      const team = teamsStore.teamById(props.teamId);
      if (team && team.productCount !== count) {
        teamsStore.upsertTeam({ ...team, productCount: count });
      }
    };

    const loadProducts = async () => {
      loading.value = true;
      listError.value = null;
      try {
        products.value = await teamsService().listTeamProducts(props.teamId);
        syncProductCount(products.value.length);
      } catch (err: any) {
        listError.value = translateError(err, 'Unable to load products');
        alertService.showHttpError(err.response ?? { status: 0, data: {} });
      } finally {
        loading.value = false;
      }
    };

    const openCreateForm = () => {
      newProductName.value = '';
      newProductDescription.value = '';
      createError.value = null;
      showCreateForm.value = true;
    };

    const cancelCreate = () => {
      showCreateForm.value = false;
      createError.value = null;
    };

    const submitCreate = async () => {
      const name = newProductName.value.trim();
      if (!name) {
        createError.value = 'Name is required';
        return;
      }
      const request: ICreateTeamProductRequest = {
        name,
        description: newProductDescription.value.trim() || null,
      };
      busy.value = true;
      createError.value = null;
      try {
        await teamsService().createTeamProduct(props.teamId, request);
        alertService.showSuccess('Product created');
        showCreateForm.value = false;
        await loadProducts();
      } catch (err: any) {
        createError.value = translateError(err, 'Could not create product');
      } finally {
        busy.value = false;
      }
    };

    const beginEdit = (product: ITeamProduct) => {
      editingId.value = product.id;
      editName.value = product.name;
      editDescription.value = product.description ?? '';
      editError.value = null;
    };

    const cancelEdit = () => {
      editingId.value = null;
      editError.value = null;
    };

    const submitEdit = async (product: ITeamProduct) => {
      const name = editName.value.trim();
      if (!name) {
        editError.value = 'Name is required';
        return;
      }
      const request: IUpdateTeamProductRequest = {
        name,
        description: editDescription.value.trim() || null,
      };
      busy.value = true;
      editError.value = null;
      try {
        const updated = await teamsService().updateTeamProduct(product, request);
        const idx = products.value.findIndex(p => p.id === product.id);
        if (idx >= 0) {
          products.value.splice(idx, 1, updated);
        }
        alertService.showSuccess('Product updated');
        editingId.value = null;
      } catch (err: any) {
        editError.value = translateError(err, 'Could not update product');
      } finally {
        busy.value = false;
      }
    };

    const setArchived = async (product: ITeamProduct, archived: boolean) => {
      busy.value = true;
      actionError.value = null;
      try {
        const updated = await teamsService().setProductArchived(product.id, archived);
        const idx = products.value.findIndex(p => p.id === product.id);
        if (idx >= 0) {
          products.value.splice(idx, 1, updated);
        }
        alertService.showSuccess(archived ? 'Product archived' : 'Product un-archived');
      } catch (err: any) {
        actionError.value = translateError(err, archived ? 'Could not archive product' : 'Could not un-archive product');
      } finally {
        busy.value = false;
      }
    };

    onMounted(loadProducts);
    watch(
      () => props.teamId,
      () => loadProducts(),
    );

    return {
      products,
      loading,
      listError,
      actionError,
      busy,
      canEdit,
      showCreateForm,
      newProductName,
      newProductDescription,
      createError,
      editingId,
      editName,
      editDescription,
      editError,
      loadProducts,
      openCreateForm,
      cancelCreate,
      submitCreate,
      beginEdit,
      cancelEdit,
      submitEdit,
      setArchived,
    };
  },
});
