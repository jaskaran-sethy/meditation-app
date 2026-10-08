# Play Console answers

Answers for the **Policy → App content** forms, based on what the app actually does. Re-check them
whenever a release adds a permission, SDK or network call.

## Privacy policy

URL: the published copy of [privacy-policy.md](privacy-policy.md), for example
`https://github.com/jaskaran-sethy/meditation-app/blob/master/docs/privacy-policy.md`. Fill in
`CONTACT_EMAIL` before publishing it. The repository must be public for Play to accept that link.

## Data safety

| Question | Answer | Why |
| --- | --- | --- |
| Does your app collect or share any of the required user data types? | **No** | Everything is kept in local SharedPreferences. The app has no `INTERNET` permission and no third-party SDKs. |

After answering "No", the form asks for nothing else and the listing shows "No data collected" and
"No data shared with third parties". This still holds with the following, which do not count as
collection:

- **Android Auto Backup**: the OS backs the data up for the user; the developer never receives it.
- **Share sheet**: the user sends the text themselves to an app they pick.
- **Notifications**: scheduled and shown on the device.

## Other declarations

| Form | Answer |
| --- | --- |
| Ads | No, the app does not contain ads |
| App access | All functionality is available without special access (no login) |
| Content rating | Questionnaire category "All other app types"; answer No to every content question (expected rating: Everyone / PEGI 3) |
| Target audience | 18+ is simplest. Including under-13s brings in the Families policy requirements. |
| Government apps | No |
| Financial features | None |
| Health apps | Select **Mindfulness / relaxation** (or the closest stress-management option). Not a medical device. |
| Permissions | `POST_NOTIFICATIONS` for daily reminders; `RECEIVE_BOOT_COMPLETED` to restore the reminder alarm after a reboot. Neither needs a separate declaration form. |
