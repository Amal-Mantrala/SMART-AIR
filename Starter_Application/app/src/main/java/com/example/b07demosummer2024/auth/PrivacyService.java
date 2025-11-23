package com.example.b07demosummer2024.auth;

public class PrivacyService {
    public String getPrivacyDefaultsExplanation() {
        return "Privacy Defaults:\n\n" +
                "By default, Providers have NO access to your children's information. " +
                "You must explicitly choose what to share with each Provider.\n\n" +
                "Default settings:\n" +
                "• All fields are private by default\n" +
                "• You control what each Provider can see\n" +
                "• Changes take effect immediately\n" +
                "• You can revoke access at any time";
    }

    public String getSharingControlsExplanation() {
        return "Parent Sharing Controls:\n\n" +
                "You have full control over what information Providers can see:\n\n" +
                "• Choose which children to share\n" +
                "• Select specific fields per child\n" +
                "• Toggle sharing on/off instantly\n" +
                "• Changes are reversible\n" +
                "• Revoke access anytime\n\n" +
                "Providers can only VIEW shared information. " +
                "They cannot edit or modify any data.";
    }

    public String getProviderInvitesExplanation() {
        return "Provider Invites:\n\n" +
                "To share information with a Provider:\n\n" +
                "1. Generate a one-time invite code/link\n" +
                "2. Share the code with your Provider\n" +
                "3. Provider accepts the invite\n" +
                "4. You then choose what to share\n\n" +
                "The Provider will only see information you explicitly enable. " +
                "You maintain full control over your privacy.";
    }
}
