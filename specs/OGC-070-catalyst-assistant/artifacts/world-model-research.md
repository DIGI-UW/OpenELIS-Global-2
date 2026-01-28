# Building a World Model for OpenELIS Global

**Created**: 2026-01-27  
**Status**: Research / Exploratory  
**Related Feature**: [OGC-070 Catalyst Assistant](../spec.md)

## Executive Summary

A world model for OpenELIS would be an **internal representation that enables AI
systems to understand, predict, and reason about laboratory information
management workflows**. This goes far beyond the schema-based RAG approach in
the Catalyst assistant (OGC-070) toward a system that can simulate lab
workflows, predict outcomes, and enable planning across complex healthcare
scenarios.

---

## 1. What is a World Model? (State-of-the-Art 2025-2026)

### Core Definition

A world model is an AI system's internal representation of how its environment
works—a learned simulator that can:

- **Predict future states** given current state + actions
- **Enable counterfactual reasoning** ("What would happen if...?")
- **Support planning** by mentally simulating action sequences
- **Maintain persistence** over long horizons

### The Three Pillars (2025 Research Consensus)

Recent comprehensive surveys identify three essential subsystems for true world
models:

| Pillar                | Function                                    | OpenELIS Application                      |
| --------------------- | ------------------------------------------- | ----------------------------------------- |
| **Generative Heart**  | Produces world states                       | Simulates sample/analysis/result states   |
| **Interactive Loop**  | Closes action-perception cycle in real-time | Responds to user actions, workflow events |
| **Persistent Memory** | Sustains coherence over long horizons       | Tracks sample lifecycle, patient history  |

### Key Insights from Leading Researchers

**LeCun's Vision**: World models should go beyond perceiving reality to envision
possible future states for decision-making and planning. For OpenELIS, this
means an AI that can reason: "If I reject this sample, what downstream effects
occur?"

**Schmidhuber's Influence**: Learning environment simulators that understand
underlying mechanisms, not just surface patterns. For OpenELIS: understanding
_why_ reflex tests trigger, not just _that_ they trigger.

**Meta's CWM (Code World Model)**: Trained on execution traces, not just static
code. For OpenELIS: learning from actual workflow traces (sample entry → results
→ validation → reporting).

### Key Research References

- **"From Masks to Worlds: A Hitchhiker's Guide to World Models"** (2025) -
  Comprehensive survey identifying five evolutionary stages of world models
- **"Understanding World or Predicting Future?"** (2024) - Survey on world model
  architectures
- **"CWM: Code World Model"** (Meta, 2025) - 32B parameter model trained on
  execution traces
- **"PAN: A World Model for General, Interactable, and Long-Horizon World
  Simulation"** (2025) - Autoregressive latent dynamics with video diffusion
- **"Dyna-Think"** (2025) - Synergizing reasoning, acting, and world model
  simulation

---

## 2. OpenELIS Domain Model (Foundation for World Model)

OpenELIS has a rich domain that is highly amenable to world modeling:

### Core Entity Graph

```
Patient ──< SampleHuman >── Sample
                              │
                              ├── accessionNumber
                              ├── status: [Entered → Started → Finished/Canceled/Disposed]
                              │
                              └──< SampleItem (specimens)
                                     │
                                     ├── TypeOfSample (Blood, Urine, etc.)
                                     ├── quantity / remainingQuantity (aliquoting)
                                     │
                                     └──< Analysis (test executions)
                                            │
                                            ├── status: [NotStarted → TechnicalAcceptance → Finalized]
                                            ├── Test (catalog definition)
                                            ├── triggeredReflex (boolean)
                                            ├── parentAnalysis / children (reflex chains)
                                            │
                                            └──< Result
                                                   ├── value, resultType
                                                   ├── minNormal, maxNormal
                                                   └── TestReflex triggers
```

### State Machines (Critical for World Model)

**Sample Status**:

```
Entered → Started → Finished
   ↓         ↓
Canceled  Canceled
   ↓
Disposed
```

**Analysis Status**:

```
NotStarted → TechnicalAcceptance → Finalized
     ↓              ↓
SampleRejected  TechnicalRejected → BiologistRejected
     ↓
  Canceled
```

### Reflex Testing Logic (Emergent Behavior)

The reflex testing system is a prime example of complex emergent behavior:

- Result value matches `TestReflex` rule → triggers new Analysis
- Parent-child Analysis relationships form reflex chains
- Scriptlets can execute custom logic
- **This is exactly the type of causality a world model must learn**

---

## 3. Architecture for an OpenELIS World Model

### Layer 1: Entity Ontology Layer (Foundation)

Following the 2025 Enterprise Ontology approach, the first layer formalizes
OpenELIS concepts:

```yaml
# OpenELIS Entity Ontology (conceptual)
entities:
  Sample:
    properties: [accessionNumber, collectionDate, receivedDate, status]
    states: [Entered, Started, Finished, Canceled, Disposed]
    transitions:
      - from: Entered
        to: Started
        trigger: FirstAnalysisStarted
      - from: Started
        to: Finished
        trigger: AllAnalysesDone

  Analysis:
    properties: [status, test, sampleItem, result, triggeredReflex]
    states: [NotStarted, TechnicalAcceptance, Finalized, Rejected, Canceled]
    parent: SampleItem
    children: [Result, ChildAnalysis]

  relationships:
    - Sample CONTAINS SampleItem
    - SampleItem CONTAINS Analysis
    - Analysis PRODUCES Result
    - Result MAY_TRIGGER TestReflex
    - TestReflex CREATES Analysis
```

### Layer 2: Workflow Dynamics Layer

This layer captures how entities transition through workflows:

```python
# Conceptual workflow dynamics model
class LabWorkflowDynamics:
    """
    Predicts next state given current state + action.
    Core of the world model.
    """

    def predict_next_state(
        self, current_state: LabState, action: LabAction
    ) -> LabState:
        """
        Examples:
        - action: EnterResult(analysis_id, value)
          → Updates Analysis status, may trigger reflexes

        - action: ValidateResult(analysis_id)
          → Finalizes Analysis, checks Sample completion

        - action: RejectSample(sample_id, reason)
          → Cascades to all SampleItems and Analyses
        """
        pass

    def simulate_trajectory(
        self, initial_state: LabState, actions: List[LabAction]
    ) -> List[LabState]:
        """
        Multi-step simulation for planning.
        """
        states = [initial_state]
        for action in actions:
            states.append(self.predict_next_state(states[-1], action))
        return states

    def counterfactual(
        self, observed_state: LabState, hypothetical_action: LabAction
    ) -> LabState:
        """
        "What if we had done X instead?"
        Critical for decision support.
        """
        pass
```

### Layer 3: Learned Model Layer

This is where modern AI techniques come in:

```python
class OpenELISWorldModel:
    """
    Neural world model trained on historical OpenELIS data.
    """

    def __init__(self):
        # State encoder: Entities → Latent representation
        self.state_encoder = EntityGraphEncoder()

        # Dynamics model: Predicts next latent state
        self.dynamics_model = TransformerDynamics()

        # Decoder: Latent → Observable entities
        self.state_decoder = EntityGraphDecoder()

        # Memory: Long-horizon coherence
        self.memory = PersistentMemoryModule()

    def encode_state(self, entities: Dict[str, List[Entity]]) -> LatentState:
        """
        Encode current lab state into latent representation.
        Handles: Samples, Analyses, Results, Patients, etc.
        """
        pass

    def predict(self, latent_state: LatentState, action: Action) -> LatentState:
        """
        Core dynamics prediction in latent space.
        """
        # Update memory with current state
        self.memory.update(latent_state)

        # Predict next state
        action_embedding = self.action_encoder(action)
        next_latent = self.dynamics_model(
            latent_state, action_embedding, self.memory.context
        )

        return next_latent

    def decode_state(self, latent_state: LatentState) -> Dict[str, List[Entity]]:
        """
        Decode back to observable entities.
        """
        pass
```

---

## 4. Training Data & Learning Strategy

### Data Sources from OpenELIS

| Data Source                                   | What It Provides        | World Model Use            |
| --------------------------------------------- | ----------------------- | -------------------------- |
| **Audit logs** (`sys_user_id`, `lastupdated`) | Who did what, when      | Action sequence learning   |
| **Status transitions**                        | State changes over time | Dynamics learning          |
| **Reflex test triggers**                      | Causal chains           | Emergent behavior learning |
| **Sample → Result timelines**                 | End-to-end workflows    | Trajectory learning        |
| **Error/rejection events**                    | Exception handling      | Edge case learning         |

### Training Approach (Meta CWM-Inspired)

Following Meta's Code World Model approach but adapted for lab workflows:

```python
class WorldModelTrainer:
    """
    Train world model on observation-action trajectories.
    """

    def create_training_data(
        self, historical_data: LabDatabase
    ) -> List[Trajectory]:
        """
        Extract trajectories from historical lab data.

        Each trajectory is:
        - Initial state (Sample created)
        - Sequence of (action, next_state) pairs
        - Final state (Sample completed/canceled)
        """
        trajectories = []

        for sample in historical_data.get_completed_samples():
            trajectory = Trajectory()
            trajectory.initial_state = self.reconstruct_state(sample, t=0)

            # Get all events in chronological order
            events = self.get_sample_events(sample)

            for event in events:
                action = self.event_to_action(event)
                next_state = self.reconstruct_state(sample, t=event.timestamp)
                trajectory.add_step(action, next_state)

            trajectories.append(trajectory)

        return trajectories

    def train(self, trajectories: List[Trajectory]):
        """
        Train dynamics model to predict next state from (state, action).

        Loss function:
        - Entity state prediction (status, values)
        - Relationship prediction (reflex chains)
        - Temporal coherence (memory consistency)
        """
        pass
```

---

## 5. Practical Applications

### A. Intelligent Workflow Prediction

```python
# Use case: Predict sample completion time
def predict_completion_time(world_model, sample: Sample) -> datetime:
    """
    Simulate forward to predict when sample will complete.
    """
    current_state = world_model.encode_state(sample.get_current_state())

    # Simulate typical workflow actions
    simulated_states = world_model.simulate_typical_workflow(current_state)

    # Find state where all analyses are Finalized
    for state in simulated_states:
        if state.is_sample_complete():
            return state.predicted_timestamp

    return None
```

### B. Anomaly Detection

```python
# Use case: Detect workflow anomalies
def detect_workflow_anomaly(
    world_model, observed_trajectory: List[LabState]
) -> List[Anomaly]:
    """
    Compare observed states against model predictions.
    Flag significant deviations.
    """
    anomalies = []

    for i, observed_state in enumerate(observed_trajectory[1:]):
        # What did the model predict?
        predicted_state = world_model.predict(
            observed_trajectory[i], observed_trajectory[i].action_taken
        )

        # Compare prediction vs observation
        deviation = world_model.compute_deviation(predicted_state, observed_state)

        if deviation > ANOMALY_THRESHOLD:
            anomalies.append(
                Anomaly(
                    timestamp=observed_state.timestamp,
                    expected=predicted_state,
                    actual=observed_state,
                    deviation=deviation,
                )
            )

    return anomalies
```

### C. Counterfactual Decision Support

```python
# Use case: "What if" analysis for lab managers
def what_if_analysis(
    world_model, sample: Sample, hypothetical_action: str
) -> dict:
    """
    Answer: "What would happen if we did X?"
    """
    current_state = world_model.encode_state(sample)

    # Simulate hypothetical action
    hypothetical_state = world_model.counterfactual(
        current_state, hypothetical_action
    )

    return {
        "action": hypothetical_action,
        "predicted_outcomes": world_model.decode_state(hypothetical_state),
        "downstream_effects": world_model.trace_effects(
            current_state, hypothetical_state
        ),
        "time_to_completion": world_model.predict_completion(hypothetical_state),
    }


# Example usage:
result = what_if_analysis(model, sample, "RejectSampleItem('item-001')")
# Returns:
# - Which analyses would be canceled
# - Effect on sample completion
# - Suggested alternative actions
```

### D. Enhanced Catalyst Assistant

Building on the Catalyst feature (OGC-070), a world model could enable:

```python
# Beyond SQL generation: Workflow-aware queries
class WorldModelEnhancedCatalyst:
    """
    Natural language queries that understand lab workflow semantics.
    """

    def answer_predictive_query(self, question: str):
        """
        Example: "Will sample ABC-123 be ready by 3 PM?"

        1. Parse question intent (prediction about sample)
        2. Load current sample state
        3. Use world model to simulate forward
        4. Generate natural language answer
        """
        pass

    def answer_counterfactual_query(self, question: str):
        """
        Example: "What would happen if we added the viral load test?"

        1. Parse question intent (hypothetical scenario)
        2. Load current state
        3. Simulate with hypothetical action
        4. Describe effects in natural language
        """
        pass

    def suggest_next_actions(self, context: LabContext) -> List[SuggestedAction]:
        """
        Proactive suggestions based on predicted optimal paths.

        Uses world model to:
        - Predict consequences of each possible action
        - Rank by utility (completion time, quality, cost)
        - Present top recommendations
        """
        pass
```

---

## 6. Technical Implementation Roadmap

### Phase 1: Ontology & Schema Grounding (Leverages Catalyst M0)

```
Tasks:
1. Formalize OpenELIS entity ontology (extend SchemaMetadata from Catalyst)
2. Define state machine specifications for all entities
3. Create relationship graph with cardinality and constraints
4. Document causal relationships (e.g., Result → TestReflex → Analysis)

Output: JSON-LD/OWL ontology file + entity graph schema
```

### Phase 2: Historical Trajectory Extraction

```
Tasks:
1. Build ETL pipeline to extract workflow trajectories from:
   - Audit tables (status changes, user actions)
   - Entity timestamps (created, updated)
   - Relationship events (sample-patient links, reflex triggers)

2. Normalize trajectories into training format:
   [initial_state, (action₁, state₁), (action₂, state₂), ...]

3. Create dataset splits (train/val/test by date to avoid leakage)

Output: Trajectory dataset (~100k+ trajectories for meaningful learning)
```

### Phase 3: Model Architecture & Training

```
Options (by complexity):

Option A: Rule-Based World Model (Simplest)
- Encode state machines and business rules explicitly
- Use rule engine for prediction
- 100% interpretable, no training needed
- Limited to known patterns

Option B: Hybrid Neuro-Symbolic (Recommended)
- Rule-based backbone for known dynamics
- Neural components for learned patterns (turnaround times, reflex probabilities)
- Combines interpretability + learning

Option C: Full Neural World Model (Most Ambitious)
- Transformer-based dynamics model
- Learns entirely from data
- Highest capacity, requires most data
- Less interpretable
```

### Phase 4: Integration with OpenELIS

```
Integration Points:
1. Real-time state encoding (subscribe to entity changes)
2. Prediction API (REST endpoint for world model queries)
3. Catalyst enhancement (add predictive/counterfactual capabilities)
4. Dashboard widgets (predicted completion times, anomaly alerts)
```

---

## 7. Challenges & Considerations

### Healthcare-Specific Challenges

| Challenge                 | Mitigation                                                               |
| ------------------------- | ------------------------------------------------------------------------ |
| **Patient privacy**       | World model operates on anonymized trajectories; no PHI in model weights |
| **Regulatory compliance** | Audit all predictions; human-in-the-loop for decisions                   |
| **High-stakes decisions** | Confidence calibration; uncertainty quantification                       |
| **Rare events**           | Synthetic data augmentation for edge cases                               |

### Technical Challenges

| Challenge                                   | Approach                                                       |
| ------------------------------------------- | -------------------------------------------------------------- |
| **Long horizons** (samples can span weeks)  | Memory-augmented models per 2026 research                      |
| **Partial observability**                   | Latent state inference; probabilistic predictions              |
| **Non-stationarity** (lab practices change) | Continual learning; drift detection                            |
| **Multi-site variation**                    | Configuration-driven model variants (Constitution Principle I) |

---

## 8. Relationship to Catalyst (OGC-070)

The Catalyst assistant and world model are complementary:

| Capability     | Catalyst (Current)     | World Model (Future)       |
| -------------- | ---------------------- | -------------------------- |
| **Query type** | SQL generation from NL | Workflow reasoning from NL |
| **Time scope** | Point-in-time queries  | Temporal predictions       |
| **Reasoning**  | Schema-grounded        | Causally-grounded          |
| **Output**     | Database results       | Predictions + explanations |

**Integration Path**:

1. **M0-M2**: Catalyst focuses on text-to-SQL (current plan)
2. **M3+**: Add world model as additional MCP tool
3. **Agent Architecture**:
   - SchemaAgent (existing) → SQL queries
   - WorldModelAgent (new) → Predictions, counterfactuals

---

## 9. Research Directions

Based on 2025-2026 state-of-the-art:

### Near-Term (Buildable Now)

1. **Rule-based world simulator** for OpenELIS workflows
2. **Entity graph embeddings** for state representation
3. **Simple dynamics prediction** (next status, completion time)

### Medium-Term (6-12 Months)

1. **Learned dynamics model** trained on historical trajectories
2. **Counterfactual reasoning engine** for "what-if" analysis
3. **Anomaly detection** using prediction residuals

### Long-Term (Research Frontier)

1. **Full generative world model** (à la LeCun's vision)
2. **Multi-step planning** for workflow optimization
3. **Cross-site transfer learning** (adapt to new lab configurations)
4. **Active learning** from user feedback

---

## 10. Summary

Building a world model for OpenELIS would involve:

1. **Formalizing the domain ontology** - leveraging the rich entity model
   (Sample, Analysis, Result, TestReflex) and state machines already in the
   codebase

2. **Extracting workflow trajectories** from historical data to learn dynamics

3. **Building a predictive model** that can simulate lab workflows forward in
   time

4. **Enabling counterfactual reasoning** for decision support ("What if we
   reject this sample?")

5. **Integrating with Catalyst** to extend beyond SQL queries to workflow-aware
   predictions

The OpenELIS domain is particularly well-suited for world modeling because:

- Clear entity relationships and state machines
- Rich historical data (audit logs, status transitions)
- Causal structures (reflex testing chains)
- Concrete success criteria (sample completion, turnaround times)

This represents the frontier of AI-powered LIMS systems—moving from reactive
query answering to proactive, predictive, planning-capable assistants that truly
understand laboratory workflows.

---

## References

### World Model Research

1. "From Masks to Worlds: A Hitchhiker's Guide to World Models" (arXiv, 2025)
2. "Understanding World or Predicting Future? A Comprehensive Survey of World
   Models" (arXiv, 2024)
3. "On Memory: A comparison of memory mechanisms in world models" (arXiv, 2025)
4. "CWM: An Open-Weights LLM for Research on Code Generation with World Models"
   (Meta AI, 2025)
5. "PAN: A World Model for General, Interactable, and Long-Horizon World
   Simulation" (arXiv, 2025)
6. "Dyna-Think: Synergizing Reasoning, Acting, and World Model Simulation in AI
   Agents" (arXiv, 2025)
7. "Planning with Reasoning using Vision Language World Model" (arXiv, 2025)

### Enterprise AI

1. "2025 Enterprise Ontology Playbook: Building a World Model for AI Agents"
   (Galaxy, 2025)
2. "World Models in Healthcare: Current Applications and Future Directions"
   (arXiv, 2025)

### OpenELIS Domain

- OpenELIS Global Constitution v1.8.1
- OGC-070 Catalyst Assistant Specification
- OpenELIS Entity Model (valueholder packages)
