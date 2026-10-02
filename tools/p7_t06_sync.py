from pathlib import Path

path = Path("docs/DECISIONS.md")
text = path.read_text(encoding="utf-8")
old = """## Adding or changing a decision

| D-084 | Accepted | P7-T06 / Issue #424 separates stable authored entity identity from transient runtime identity. Public immutable `engine-world` `EntityGuid(highBits,lowBits)` is the stable 128-bit authoring/persistence value with JDK UUID generation and strict canonical lowercase UUID text. `EntityId(index,generation)` remains runtime-only. Package-private `EntityGuidIndex` maps one stable GUID to one currently live exact `EntityId` for one allocator context, rejects conflicting bindings, removes dead mappings safely, and never transfers identity across slot reuse. Authored cross-entity references retain GUIDs and resolve them to current runtime IDs at load/runtime binding boundaries. P7-T06 defines no scene JSON fields/schema/version, parent representation, public `World`, or replication/network identity. | Persisting generational runtime handles would couple authored references to allocator history and slot reuse. A typed stable GUID prevents identity-domain mixups and gives later P7-T07 scene loading a deterministic resolution key without freezing its JSON schema or a public world lifecycle prematurely. |

An Issue must explicitly authorize a durable architecture change."""
new = """| D-084 | Accepted | P7-T06 / Issue #424 separates stable authored entity identity from transient runtime identity. Public immutable `engine-world` `EntityGuid(highBits,lowBits)` is the stable 128-bit authoring/persistence value with JDK UUID generation and strict canonical lowercase UUID text. `EntityId(index,generation)` remains runtime-only. Package-private `EntityGuidIndex` maps one stable GUID to one currently live exact `EntityId` for one allocator context, rejects conflicting bindings, removes dead mappings safely, and never transfers identity across slot reuse. Authored cross-entity references retain GUIDs and resolve them to current runtime IDs at load/runtime binding boundaries. P7-T06 defines no scene JSON fields/schema/version, parent representation, public `World`, or replication/network identity. | Persisting generational runtime handles would couple authored references to allocator history and slot reuse. A typed stable GUID prevents identity-domain mixups and gives later P7-T07 scene loading a deterministic resolution key without freezing its JSON schema or a public world lifecycle prematurely. |

## Adding or changing a decision

An Issue must explicitly authorize a durable architecture change."""
count = text.count(old)
if count != 1:
    raise RuntimeError(f"expected one misplaced D-084 block, found {count}")
path.write_text(text.replace(old, new, 1).rstrip() + "\n", encoding="utf-8")
