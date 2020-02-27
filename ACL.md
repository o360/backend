# Access-Control List

| Entity | Action | User | Admin |
| ------ | ------ | ---- | ----- |
| User | Get list | - | + |
|  | Get by ID | <details><summary>+</summary><br>ID = logged in user ID</details>  | + |
|  | Update | <details><summary>+</summary><br>ID = logged in user ID<br>Available fields:<li>name,</li><li>email</li></details>| + |
|  | Delete | - | + |
| Group | Get by ID, Get list, Create, Update, Delete | - | + |
| Group-user | Add user to group | - | + |
|  | Remove user from group | - | + |
| Event | Get list | <details><summary>+</summary><br>Only available for user events</details> | + |
|  | Get by ID | + | + |
|  | Create, Update, Delete, Clone | - | + |
| Event-project | Create, Delete | - | + |
| Form | Get By ID | <details><summary>+</summary><br>Only if form is freezed and user is in relation with this form</details> | + |
|  | Get list, Create, Update, Delete, Clone | - | + |
| Project | Get List | <details><summary>+</summary><br>Only if user is in assessing group in project</details> | + |
|  | Get by ID, Create, Update, Delete | - | + |
| Relation | Get list, Get by ID, Create, Update, Delete | - | + |
| Template | Get list, Get by ID, Create, Update, Delete | - | + |
| Assessment | Get List | <details><summary>+</summary><br>Only if user is in assessing group in project and project is in event</details> | <details><summary>+</summary><br>Only if user is in assessing group in project and project is in event</details> |
|  | Submit | <details><summary>+</summary><br>Only if user is in assessing group in project and project is in event</details> | <details><summary>+</summary><br>Only if user is in assessing group in project and project is in event</details> |
