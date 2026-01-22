# emitterMap의 멀티스레딩 사용시 에러

emitterMap을 heartBeat, printInfo에서 동시에 사용하는 경우가 생긴다.

이때 클라이언트 쪽에서 sse 연결을 끊고, heartBeat나 printInfo에서 메시지를 전송한다.

이때 메시지 전송에 실패하고 콜백 메서드가 실행되어 sse 연결이 정리된다. ( completeWithErrors() )
emitterMap의 emitter가 삭제되지 않은 상태에서, 다른 스레드가 이 연결 객체를 사용하려고 하면, `IllegalStateException`이 발생한다.

즉, 한 스레드에서 연결 종료 처리를 했는데, 이후에 콜백 메서드가 완료되기 전에 다른 스레드에서 이를 사용해서 문제가 발생한 것.

전형적인 멀티스레딩 경쟁 상태 버그이다.

다행인 점은, 클라이언트로부터 연결이 끊기고 처음 메시지를 보내는 경우에는 `IOException`이 발생하고, 그 다음에 연결 객체를 사용할 때는 `IllegalStateException`이
발생해서 처리하기 용이하다.

즉, `IllegalStateException`이 발생하면 그냥 무시하고 다른 스레드가 연결을 끊기를 기다리면 되는 것이다.
