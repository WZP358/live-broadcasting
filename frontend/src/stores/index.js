import { useWebStore } from "./modules/web";
import { useUserStore } from "./modules/user";

export const useStore = () => ({
    web: useWebStore,
    user: useUserStore
})
