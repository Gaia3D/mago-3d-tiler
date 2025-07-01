How to use git subtree
---

### [remote]
```
git remote add io https://github.com/Gaia3D/mago-io.git
```

### [add subtree]
```
git subtree add --prefix=io https://github.com/Gaia3D/mago-io.git main
```

### [pull]
```
git subtree pull --prefix=io https://github.com/Gaia3D/mago-io.git main
```

### [push]
```
git subtree push --prefix=io https://github.com/Gaia3D/mago-io.git main
```
