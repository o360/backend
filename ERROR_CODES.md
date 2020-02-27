| Error code | Details |
| ---------- | ------- |
| **401** |
|PROVIDER_NOT_SUPPORTED | OAuth provider is not supported |
|GENERAL_AUTHENTICATION | Missed token, invalid token, expired token |
| **403** |
| AUTHORIZATION-1 | Authorization failed. For more details see Бизнес-ограничения |
| AUTHORIZATION-2 | Authorization failed. Action is allowed with some conditions. For more details see Бизнес-ограничения |
| AUTHORIZATION-EVENT-1 | Authorization failed. Can' t modify projects in in-progress events |
| AUTHORIZATION-EVENT-2 | Authorization failed. Can' t update completed event |
| AUTHORIZATION-FORM-1 | Can't get form template by ID |
| **400** |
| GENERAL-1 | Invalid JSON, invalid query params. For more details see message |
| PAGINATION | Invalid pagination format |
| SORTING | Invalid sorting format |
| BAD-REQUEST-EVENT-1 | Invalid event model. Start date is after end date |
| BAD-REQUEST-EVENT-2 | Invalid event model. Duplicate notifications |
| BAD-REQUEST-EVENT-3 | Can't use past dates in event |
| BAD-REQUEST-RELATION-1 | Can't change relation project ID |
| BAD-REQUEST-RELATION-2 | Duplicate relation in project |
| BAD-REQUEST-RELATION-3 | Group to missed in classic relation |
| BAD-REQUEST-ASSESSMENT-1 | Invalid form or answers |
| BAD-REQUEST-ASSESSMENT-2 | Required answers missed |
| BAD-REQUEST-ASSESSMENT-3 | Self voting |
| BAD-REQUEST-ASSESSMENT-4 | Error aggregating multiple errors |
| BAD-REQUEST-FILE-1 | Invalid file extension |
| BAD-REQUEST-FILE-2 | Unexpected problems with file upload |
| BAD-REQUEST-FILE-3 | Invalid file upload request |
| **404** |
| GENERAL-1 | Endpoint not found or endpoint found, but method not allowed |
| NOTFOUND-USER | User not found |
| NOTFOUND-GROUP | Group not found |
| NOTFOUND-FORM | Form not found |
| NOTFOUND-PROJECT | Project not found |
| NOTFOUND-RELATION | Relation not found |
| NOTFOUND-EVENT | Event not found |
| NOTFOUND-TEMPLATE | Template not found |
| NOTFOUND-ASSESSMENT | Assessment not found |
| NOTFOUND-INVITE | Invite not found (by code) |
| NOTFOUND-ACTIVEPROJECT | Active project not found |
| **409** |
| CONFLICT-GENERAL | Might happen in unsafe methods. Unspecified data integrity violation, e.g. foreign key constraints violation |
| CONFLICT-GROUP-1 | Might happen in group updating. Group can't be parent to itself |
| CONFLICT-GROUP-2 | Might happen in group creating or updating. Circular reference parent-child detected |
| CONFLICT-GROUP-5 | Can't delete, relation exists |
| CONFLICT-GROUP-6 | Duplicate name |
| CONFLICT-GROUP-GENERAL | Might happen in group deleting. Relations or project exists |
| CONFLICT-USER-GENERAL | Might happen in user deleting. User is in a group(s) |
| CONFLICT-USER-2 | Can't add unapproved user to group |
| CONFLICT-FORM-1 | Might happen in form creating or updating. Missed value list for form element, e.g. for element with type 'radio' |
| CONFLICT-FORM-3 | Might happen in form updating. Active events with this form exists |
| CONFLICT-FORM-5 | Can't use, update or delete freezed form |
| CONFLICT-FORM-6 | Wrong values for like-dislike form element |
| CONFLICT-FORM-GENERAL | Might happen in form deleting. Exists relations with this form |
| CONFLICT-PROJECT-2 | Can'not update project. Active events with this project exists |
| CONFLICT-PROJECT-3 | Can't update, active events exists |
| CONFLICT-PROJECT-4 | Duplicate name |
| CONFLICT-PROJECT-GENERAL | Can't delete project. Events with this project exists |
| CONFLICT-TEMPLATE-GENERAL | Can't delete template. Projects or relations with this template exists |
| CONFLICT-ASSESSMENT-1 | Can't find relation for user and form id |
| CONFLICT-ASSESSMENT-2 | Revoting is forbidden |
| CONFLICT-ASSESSMENT-3 | Skipping is forbidden |
| CONFLICT-ASSESSMENT-4 | Event is not in progress |
| CONFLICT-INVITE-1 | Can't send invite to already registered user |
| CONFLICT-INVITE-2 | Approved user can't submit invite codes |
| CONFLICT-INVITE-3 | Invite code already used |
| **500** |
| GENERAL-2 | Unexpected server error |
