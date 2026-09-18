import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type IComment } from '@/shared/model/comment.model';

import CommentService from './comment.service';

export default defineComponent({
  name: 'CommentDetails',
  setup() {
    const dateFormat = useDateFormat();
    const commentService = inject('commentService', () => new CommentService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const dataUtils = useDataUtils();

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const comment: Ref<IComment> = ref({});

    const retrieveComment = async commentId => {
      try {
        const res = await commentService().find(commentId);
        comment.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.commentId) {
      retrieveComment(route.params.commentId);
    }

    return {
      ...dateFormat,
      alertService,
      comment,

      ...dataUtils,

      previousState,
    };
  },
});
