package ml.docilealligator.infinityforreddit.events;

public class SubmitImagePostEvent {
    public final boolean postSuccess;
    public final String errorMessage;

    public SubmitImagePostEvent(boolean postSuccess, String errorMessage) {
        this.postSuccess = postSuccess;
        this.errorMessage = errorMessage;
    }
}
