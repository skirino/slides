## Introduction to [raft_fleet](https://github.com/skirino/raft_fleet)

桐野 俊輔 ([skirino](https://github.com/skirino)) @ [ACCESS](http://jp.access-company.com/)

2017/01/11

***

### Motivation (1)

- Erlang/Elixir: much higher-level than many other languages
    - most of low-level stuff is abstracted away
        - processes with mailboxes as building blocks
        - dead simple node-to-node communication (location transparency)

---

### Motivation (2)

- Even armed with Erlang/Elixir, it's still hard to manipulate "state" within a cluster of nodes
    - And actually we want multiple instances of such "state"s

---

### Motivation (3)

- It's hard because we need
    - data replication within multiple nodes
    - leader election mechanism (or something similar) to serialize client requests
        - and failover of a failed leader
    - process migration from a node to another (while processing client requests) for e.g. deployment
    - load-balancing of tasks (processes) within nodes

---

### Requirements (1)

- Host multiple "state"s within a cluster
    - Automatic load-balancing, reasonably scalable
- Support [Linearizable](https://en.wikipedia.org/wiki/Linearizability) semantics
    - i.e. client operations on a "state" are processed atomically in the issued order
    - also we don't want to
        - lose any acknowledged writes
        - have duplicated writes due to client retry

---

### Requirements (2)

- Tolerate failures of minority of consensus group members
    - tolerance for DC-failure is also nice to have
- Persist data for crash recovery
    - periodic cleanup of unnecessary data in disk
- Automatically recover from non-critical node failure
- Provide flexible data model for "state"s

---

### Basic design

- Use [Raft consensus algorithm](https://raft.github.io/)
- Divide Raft protocol implementation and other parts as separate libs
    - for clear separation of concerns
        - [`rafted_value`](https://github.com/skirino/rafted_value): Raft protocol implementation
        - [`raft_fleet`](https://github.com/skirino/raft_fleet): Running and managing multiple consensus groups in a cluster

---

### Related works (1)

- [mnesia](http://erlang.org/doc/man/mnesia.html)
    - replication and distributed transaction
    - table and record (tuple), data is replicated on table-basis
    - large and heavyweight; failure recovery is difficult (as far as I know)

---

### Related works (2)

- Basho's [riak_ensemble](https://github.com/basho/riak_ensemble)
    - built for similar goal (CAS in Riak)
    - (an improved variant of) Paxos consensus algorithm
    - specialized to key-value operations
    - basically a part of Riak, not a standalone lib

***
***

### [rafted_value](https://github.com/skirino/rafted_value) - Overview

- Raft protocol implementation, including membership changes
    - Each Raft member as a [`:gen_statem`](http://erlang.org/doc/man/gen_statem.html) process
- Arbitrary data structure can be replicated among members
- Commands must be pure
    - only minimal information is included in Raft logs
    - impure operations can be done in `LeaderHook` callbacks

---

### [rafted_value](https://github.com/skirino/rafted_value) - Public API design

- `start_link/2`
    - consensus member process is started in 2 ways:
        - to become a leader of a newly-created consensus group
        - to become a follower of an existing consensus group
    - in order to avoid race condition of multiple leaders
        - i.e. "whether a consensus group exists or not" must be handled by the caller

---

### [rafted_value](https://github.com/skirino/rafted_value) - Implementation detail (1)

- Core component is [`RaftedValue.Server`](https://github.com/skirino/rafted_value/blob/master/lib/rafted_value/server.ex) module
    - a `:gen_statem` with 3 states: `leader`, `candidate`, `follower`
    - need to handle 19 types of events
    - => 57 handlers to implement
- Sane module- and function-level design is the key
    - unify multiple handlers
    - hierarchical process state (nested structs)
    - utilities with well-defined responsibilities

---

### [rafted_value](https://github.com/skirino/rafted_value) - Implementation detail (2)

- For linearizability
    1. assign a unique ID to each command
    2. responses of command executions are cached
    3. if cache found for a command, don't execute the command and just return the cached response
- This is basically equivalent to implicitly establish client session for each request

---

### [rafted_value](https://github.com/skirino/rafted_value) - Testing (1)

- Really tough
- Strategy:
    - test processes within a single ErlangVM (give up multi-node tests in this layer)
    - tweak inter-process communications to simulate netsplit (using something like DI)
    - employ both ordinary [`ExUnit`](http://elixir-lang.org/docs/stable/ex_unit/ExUnit.html) tests and property-based tests

---

### [rafted_value](https://github.com/skirino/rafted_value) - Testing (2)

- Property-based tests
    - under continuous client requests,
    - repeatedly change consensus group configurations (add/remove member, replace leader, kill a member, netsplit),
    - and see whether all invariants hold or not

---

### [rafted_value](https://github.com/skirino/rafted_value) - Testing (3)

- Property-based tests
    - compared with typical `ExUnit` tests
        - much easier to find bugs
        - on failure, much harder to understand what's going on

---

### [rafted_value](https://github.com/skirino/rafted_value) - Testing (4)

- (Rather silly) bugs caught during testing
    - failed to keep struct fields
        - `%S{f: v}` instead of `%S{s | f: v}`
    - forget to reset timer
    - off-by-one bug in judging majority
    - etc.

***
***

### [raft_fleet](https://github.com/skirino/raft_fleet) - Overview

- Run multiple `rafted_value` processes within a cluster of ErlangVMs
- For this purpose, `raft_fleet`
    - defines process naming scheme
    - implements process placement algorithm ([rendezvous hashing](https://en.wikipedia.org/wiki/Rendezvous_hashing))
    - manages consensus on current cluster status (participating nodes, consensus groups)
    - automatically re-balance consensus members
- Users of `raft_fleet` define "state" and operations on it by implementing [`RaftedValue.Data`](https://hexdocs.pm/rafted_value/RaftedValue.Data.html) behaviour

---

### [raft_fleet](https://github.com/skirino/raft_fleet) - Process naming scheme

- Use `atom` as ID of consensus group
    - consensus group members are registered with the same `atom`
    - this makes much easier to keep track of where the consensus members reside
    - `atom`-only restriction may be relaxed in future releases

---

### [raft_fleet](https://github.com/skirino/raft_fleet) - Process design (1)

- Basically the same as https://github.com/basho/riak_ensemble/raw/develop/doc/cluster.png

---

### [raft_fleet](https://github.com/skirino/raft_fleet) - Process design (2)

- "cluster consensus group"
    - manages bookkeeping info of `raft_fleet`
    - works as a single source of truth; serializes operations to add/remove node/group
    - correctly bootstrapping members of cluster consensus group is crucial; use lock facility provided by [`:global`](http://erlang.org/doc/man/global.html) module
- Other consensus group members are started on demand

---

### [raft_fleet](https://github.com/skirino/raft_fleet) - [rendezvous hashing](https://en.wikipedia.org/wiki/Rendezvous_hashing) (1)

- Motivation of the algorithm
    - want to assign members to each node
    - node addition/removal triggers rebalancing
        - e.g. 100 processes in 4 nodes => 5 nodes
            - a: `24`, b: `27`, c: `23`, d: `26`
            - a: `19`, b: `23`, c: `18`, d: `21`, e: `19`
    - don't want to migrate many processes
        - ideally only `1/n_nodes` processes to migrate
        - simply taking `mod` leads to really bad results

---

### [raft_fleet](https://github.com/skirino/raft_fleet) - [rendezvous hashing](https://en.wikipedia.org/wiki/Rendezvous_hashing) (2)

- Algorithm
    - sort members using "random weight" (hash value)
    - take members with least (highest) weights
- Compared with [consistent hashing](https://en.wikipedia.org/wiki/Consistent_hashing), rendezvous hashing
    - is much simpler
    - naturally integrates data center-aware placement
    - is not flexible to tweak hotspot (in the case of e.g. key-value store)

---

### [raft_fleet](https://github.com/skirino/raft_fleet) - [rendezvous hashing](https://en.wikipedia.org/wiki/Rendezvous_hashing) (3)

```ex
defun lrw_members(nodes_per_zone :: %{ZoneInfo.t => [node]}, group :: atom, n_replica :: pos_integer) :: [node] do
  Enum.flat_map(nodes_per_zone, fn {_z, ns} ->
    Enum.map(ns, fn n -> {Hash.calc({n, group}), n} end)
    |> Enum.sort
    |> Enum.map_reduce(0, fn({hash, node}, index) -> {{index, hash, node}, index + 1} end)
    |> elem(0)
  end)
  |> Enum.sort
  |> Enum.map(fn {_i, _h, n} -> n end)
  |> Enum.take(n_replica)
end
```

---

### [raft_fleet](https://github.com/skirino/raft_fleet) - Fault tolerance

- We can't put trust on remote communications
    - e.g. spawning a new leader is done locally (without remote communication) as it must be done exactly once
- Especially avoid synchronous remote messaging
    - supervisor API is synchronous
    - don't call supervisor APIs from other node; pass information to manager process of the target node and ask it to call
- Retry operations that can go wrong...

---

### [raft_fleet](https://github.com/skirino/raft_fleet) - Automatic healing

- Run periodic checks of consensus members
    - add missing member, remove extra member, replace leader with member in the desired node
    - to avoid contention, each node manages consensus groups whose leaders are expected to be on that node
    - node with too many failing members will be purged
    - consensus group with majority failing is removed (as a last resort)

---

### [raft_fleet](https://github.com/skirino/raft_fleet) - Testing (1)

- Extremely daunting
- Strategy:
    - use [`:slave`](http://erlang.org/doc/man/slave.html) module and test against multiple local nodes
        - long-running tests...
    - test that
        - starting with various node configurations
        - under continuous client requests
        - change node configuration (add/remove/kill node)
        - and (after some time) see that consensus members are (roughly) evenly distributed across nodes

---

### [raft_fleet](https://github.com/skirino/raft_fleet) - Testing (2)

- Bugs caught during testing (really difficult to find)
    - node-to-node connections are propagated asynchronously; cannot acquire global lock for cluster consensus before they completely propagate
    - deadlock due to multiple followers calling each other
- Explicitly clearing up all resources (node, process, ETS) for each test is crucial...

***
***

### [raft_fleet](https://github.com/skirino/raft_fleet) in action (1)

- We have used `raft_fleet` to implement job queues
    - one consensus group per job queue
    - push-based communication; avoid excessive polling

---

### [raft_fleet](https://github.com/skirino/raft_fleet) in action (2)

- On adding a node to working cluster:
    - In `YourApp.start/2`,
        - connect the node to other nodes in the cluster, then
        - call `RaftFleet.activate/1`
    - The node starts to host member processes of existing consensus groups

---

### [raft_fleet](https://github.com/skirino/raft_fleet) in action (3)

- On removing a node from working cluster:
    - Call `RaftFleet.deactivate/0` before terminating ErlangVM and wait for a while
    - Member processes in the target node will be migrated to the remaining nodes

---

### [raft_fleet](https://github.com/skirino/raft_fleet) in action (4)

- Bugs caught in dev/prod environment
    - Sending messages to an already-deleted node takes really long (more than a few seconds), resulting in leader election timeout
        - `:noconnect` option of `:erlang.send/3` is crucial here

---

### [raft_fleet](https://github.com/skirino/raft_fleet) in action (5)

- Bugs caught in dev/prod environment
    - Race condition between node deactivation and leader migration: assigning a process in soon-to-be-deleted node as the next leader
        - Check the node status before choosing the next leader

***
***

### Summary

- `raft_fleet` makes cluster-wide "state"s much easier
- Proper separation of concerns and thorough testing in each layer are the only way to keep our sanity
- Comments/feedbacks/PRs are more than welcome!
