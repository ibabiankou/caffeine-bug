# In rare cases cache remains stale forever.

The script compiles the code and packages a fat jar to make it simpler to run.
Then it starts N bash terminals each running code M times in a loop.
It's more often reproduces under higher load, I run 36 sessions of 100 iterations each.
```bash
./parallel.sh N M
```