📒 mago-io
---
This repository is a common library for Gaia3D's mago-io and mago-common.
It contains shared code and resources that are used across multiple projects, ensuring consistency and reducing duplication.
This library is designed to be used as a subtree in other repositories, allowing for easy integration and updates.
---
## How to use git subtree

### [remote]
```
git remote add mago-io https://github.com/Gaia3D/mago-io.git
```

### [add subtree]
```
git subtree add --prefix=mago-io https://github.com/Gaia3D/mago-io.git main
```

### [pull]
```
git subtree pull --prefix=mago-io https://github.com/Gaia3D/mago-io.git main
```

### [push]
```
git subtree push --prefix=mago-io https://github.com/Gaia3D/mago-io.git main
```
