declare module 'js-cookie' {
  const Cookies: {
    get(name: string): string | undefined;
  };
  export default Cookies;
}

declare module 'sockjs-client' {
  class SockJS {
    constructor(url: string);
  }
  export default SockJS;
}
