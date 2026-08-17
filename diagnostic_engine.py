"""
RoomAI Diagnostic Engine v2

Purpose:
    Convert a room image into a conservative,
    evidence-based, actionable diagnosis.

Design principles:
    - Never invent exact measurements.
    - Separate observation from inference.
    - Every problem requires evidence.
    - Confidence is categorical, not fake precision.
    - Unknown is a valid answer.
    - Functional problems outrank cosmetic problems.
    - Expensive recommendations require justification.
    - The output is normalized before reaching the app.
"""

import json


ALLOWED_CONFIDENCE = {
    "high",
    "medium",
    "low",
    "unknown",
}

ALLOWED_SEVERITY = {
    "low",
    "medium",
    "high",
}

ALLOWED_IMPACT = {
    "low",
    "medium",
    "high",
}

ALLOWED_FEASIBILITY = {
    "easy",
    "moderate",
    "hard",
    "unknown",
}

ALLOWED_PROBLEM_TYPES = {
    "space",
    "layout",
    "movement",
    "lighting",
    "storage",
    "access",
    "ergonomics",
    "safety",
    "cleaning",
    "installation",
    "privacy",
    "function",
    "visual_balance",
    "color",
    "decor",
    "other",
}

ALLOWED_QUALITY = {
    "good",
    "acceptable",
    "poor",
    "insufficient",
}


def build_diagnostic_prompt(user_goal=""):
    goal = user_goal.strip()

    goal_text = (
        goal
        if goal
        else "The user has not specified a goal."
    )

    return f"""
You are RoomAI Diagnostic Engine v2.

You are NOT an interior-design image generator.

Your primary job is to diagnose practical problems in a real room
and recommend the smallest useful intervention.

USER GOAL:
{goal_text}

IMPORTANT:
A visually attractive room is not necessarily a functional room.

You must reason from visible evidence.

==================================================
CORE RULES
==================================================

QUALITY AND EVIDENCE GATES
==================================================

NO FABRICATED MEASUREMENTS:
Never invent, infer, or present exact room measurements,
distances, dimensions, angles, areas, or clearances unless
the user explicitly supplied those measurements or reliable
measurement data is available.

Visual estimation is NOT a measurement.

If a dimension matters but cannot be verified:
- say that it cannot be verified from the image
- use qualitative language such as "appears narrow"
- set requires_measurement = true when appropriate

EVERY PROBLEM NEEDS EVIDENCE:
Every diagnosed problem MUST be grounded in visible evidence
from the image or information explicitly supplied by the user.

Do not output a problem based only on assumptions,
generic interior-design rules, or imagined room geometry.

OBSERVATION:
Describe only what is visibly supported.

INFERENCE:
Explain what the observation may imply.

Never present an inference as a directly observed fact.

QUALITY GATE:
If the image is too dark, blurry, obstructed, incomplete,
or otherwise insufficient to support a reliable diagnosis,
do not invent problems.

Return an insufficient/poor quality result and leave
problems empty when reliable evidence is unavailable.

UNKNOWN:
When evidence is insufficient for a conclusion, use
confidence = "unknown" rather than guessing.

==================================================
1. NEVER invent exact measurements.

Do not claim:
- "the walkway is exactly 65 cm"
- "the room is 3.2 meters wide"
- "the desk is 80 cm from the wall"

unless the user supplied those measurements.

Instead use:
- appears narrow
- appears wide enough
- likely constrained
- cannot be verified from one image

2. Separate:
OBSERVATION
from
INFERENCE.

Example:

Observation:
"The desk appears close to the bed."

Inference:
"This may restrict circulation."

3. Every detected problem MUST contain evidence.

Bad:
"Poor layout."

Good:
"The desk appears to occupy the main path between the bed
and the room entrance."

4. Confidence must describe visual evidence quality.

Use ONLY:
- high
- medium
- low
- unknown

Do NOT output percentages.

5. If a conclusion cannot be supported:
confidence = "unknown"

Do not force a diagnosis.

6. Functional problems have priority over cosmetic problems.

Priority order normally follows:

movement/access
safety
function
storage
ergonomics
lighting
cleaning
visual balance
color/decor

But user goal can change the ranking.

7. Do not recommend expensive replacement when rearrangement,
reuse or a low-cost intervention could solve the problem.

8. Preserve useful existing furniture.

9. Do not diagnose structural or safety issues as facts
unless the visual evidence is strong.
If uncertain, explicitly mark the uncertainty.

10. A single image cannot reliably establish:
- exact dimensions
- structural integrity
- electrical safety
- load capacity
- hidden storage capacity
- exact sunlight levels over the whole day

Mark these as unknown or request a user measurement/detail.

11. Do not praise the room unless it helps explain the diagnosis.

12. Avoid generic advice such as:
"add plants"
"add art"
"make it modern"
unless the user's actual problem requires it.

==================================================
QUALITY GATE
==================================================

Before diagnosing the room, evaluate:

- Is this actually an indoor room?
- Is the room sufficiently visible?
- Is the image too dark?
- Is the image too blurry?
- Is the room mostly blocked/occluded?
- Are important walls/floor/openings visible enough?
- Is the perspective usable?

Return:

quality:
{{
  "status": "good|acceptable|poor|insufficient",
  "reason": "...",
  "can_diagnose": true
}}

If the image is insufficient for reliable diagnosis:

can_diagnose = false

and provide:
- what is missing
- what photo should be taken
- whether another angle is needed

Do NOT invent problems from an insufficient image.

==================================================
SCENE OBSERVATIONS
==================================================

Identify only objects and structural elements that are visibly supported.

Examples:

- bed
- sofa
- desk
- table
- wardrobe
- cabinet
- chair
- window
- door
- radiator
- TV
- shelves
- floor
- wall
- ceiling

For each important object provide:

- id
- category
- visible_location
- confidence

Do not estimate exact dimensions.

==================================================
SPATIAL REASONING
==================================================

Reason about:

- circulation
- adjacency
- blocked access
- furniture crowding
- functional zones
- likely workspace conflicts
- likely storage limitations
- furniture-to-opening relationships

But distinguish:
visible fact
from
likely consequence.

==================================================
PROBLEM DETECTION
==================================================

Each problem must contain:

{{
  "id": "problem_1",
  "type": "...",
  "title": "...",
  "observation": "...",
  "evidence": "...",
  "reason": "...",
  "confidence": "high|medium|low|unknown",
  "impact": "high|medium|low",
  "severity": "high|medium|low",
  "feasibility": "easy|moderate|hard|unknown",
  "recommendation": "...",
  "requires_measurement": false,
  "requires_user_answer": false
}}

Only use problem types:

space
layout
movement
lighting
storage
access
ergonomics
safety
cleaning
installation
privacy
function
visual_balance
color
decor
other

==================================================
PRIORITY
==================================================

Assign a priority score from 0 to 100.

This is a ranking heuristic, NOT a probability.

Consider:

- impact
- confidence
- feasibility
- user goal
- cost sensitivity
- whether the problem blocks other improvements

Do not use false mathematical precision.

Also provide:

priority_reason

Example:
"High impact, high visual confidence and zero-cost rearrangement."

==================================================
KEEP / REPLACE / UPGRADE
==================================================

KEEP:
Existing items that are useful and should probably remain.

REPLACE:
Only items whose replacement is justified by the diagnosed problem.

UPGRADE:
Items that can be improved without unnecessary replacement.

==================================================
USER QUESTIONS
==================================================

Ask a question only when its answer can materially change
the recommendation.

Examples:

- "Do you need this desk for daily work?"
- "Can the desk be moved?"
- "How wide is the room approximately?"
- "Do you need more clothing storage?"

Do NOT ask unnecessary questions.

==================================================
FINAL RESPONSE
==================================================

Return ONLY valid JSON.

Schema:

{{
  "schema_version": "2.0",
  "quality": {{
    "status": "good|acceptable|poor|insufficient",
    "reason": "...",
    "can_diagnose": true
  }},
  "room": {{
    "type": "...",
    "confidence": "high|medium|low|unknown"
  }},
  "summary": "...",
  "score": 0,
  "observations": [],
  "problems": [],
  "risk_scanner": [],
  "keep": [],
  "replace": [],
  "upgrade": [],
  "priority_actions": [],
  "lifestyle_questions": []
}}

score:
Use a practical room-function score from 0 to 100.
Do not interpret it as scientific measurement.

If quality.can_diagnose is false:
- problems must be []
- priority_actions must be []
- explain what is missing in quality.reason
"""


def _string(value, default=""):
    if value is None:
        return default

    return str(value).strip()


def _choice(value, allowed, fallback):
    value = _string(value).lower()

    if value in allowed:
        return value

    return fallback


def _bool(value):
    return bool(value) if isinstance(value, bool) else False


def normalize_diagnosis(raw):
    """
    Defensive normalization.

    The model can still make a semantic mistake,
    but malformed fields and unsupported categories
    are removed before the Android client sees them.
    """

    if not isinstance(raw, dict):
        raise ValueError(
            "Diagnosis must be a JSON object"
        )

    quality_raw = raw.get("quality")

    if not isinstance(quality_raw, dict):
        quality_raw = {}

    quality_status = _choice(
        quality_raw.get("status"),
        ALLOWED_QUALITY,
        "insufficient"
    )

    quality = {
        "status": quality_status,
        "reason": _string(
            quality_raw.get("reason"),
            "Image quality could not be established."
        ),
        "can_diagnose": (
            _bool(quality_raw.get("can_diagnose"))
            and quality_status in {
                "good",
                "acceptable"
            }
        )
    }

    room_raw = raw.get("room")

    if not isinstance(room_raw, dict):
        room_raw = {}

    room = {
        "type": _string(
            room_raw.get("type"),
            "unknown"
        ),
        "confidence": _choice(
            room_raw.get("confidence"),
            ALLOWED_CONFIDENCE,
            "unknown"
        )
    }

    observations = []

    raw_observations = raw.get(
        "observations",
        []
    )

    if isinstance(raw_observations, list):

        for item in raw_observations:

            if not isinstance(item, dict):
                continue

            observations.append({
                "id": _string(
                    item.get("id"),
                    "observation_%d"
                    % (len(observations) + 1)
                ),
                "category": _string(
                    item.get("category"),
                    "unknown"
                ),
                "visible_location": _string(
                    item.get("visible_location"),
                    "unknown"
                ),
                "confidence": _choice(
                    item.get("confidence"),
                    ALLOWED_CONFIDENCE,
                    "unknown"
                )
            })

    problems = []

    raw_problems = raw.get(
        "problems",
        []
    )

    if isinstance(raw_problems, list):

        for item in raw_problems:

            if not isinstance(item, dict):
                continue

            confidence = _choice(
                item.get("confidence"),
                ALLOWED_CONFIDENCE,
                "unknown"
            )

            evidence = _string(
                item.get("evidence")
            )

            observation = _string(
                item.get("observation")
            )

            # A problem without evidence is not allowed
            # to become an actionable diagnosis.
            if not evidence:
                continue

            problem_type = _choice(
                item.get("type"),
                ALLOWED_PROBLEM_TYPES,
                "other"
            )

            problems.append({
                "id": _string(
                    item.get("id"),
                    "problem_%d"
                    % (len(problems) + 1)
                ),
                "type": problem_type,
                "title": _string(
                    item.get("title"),
                    "Room issue"
                ),
                "observation": observation,
                "evidence": evidence,
                "reason": _string(
                    item.get("reason")
                ),
                "confidence": confidence,
                "impact": _choice(
                    item.get("impact"),
                    ALLOWED_IMPACT,
                    "medium"
                ),
                "severity": _choice(
                    item.get("severity"),
                    ALLOWED_SEVERITY,
                    "medium"
                ),
                "feasibility": _choice(
                    item.get("feasibility"),
                    ALLOWED_FEASIBILITY,
                    "unknown"
                ),
                "recommendation": _string(
                    item.get("recommendation")
                ),
                "requires_measurement": _bool(
                    item.get("requires_measurement")
                ),
                "requires_user_answer": _bool(
                    item.get("requires_user_answer")
                )
            })

    # If the image cannot be diagnosed reliably,
    # no actionable problem is allowed through.
    if not quality["can_diagnose"]:
        problems = []

    priority_actions = []

    raw_actions = raw.get(
        "priority_actions",
        []
    )

    if isinstance(raw_actions, list):

        for item in raw_actions:

            if not isinstance(item, dict):
                continue

            confidence = _choice(
                item.get("confidence"),
                ALLOWED_CONFIDENCE,
                "unknown"
            )

            # Do not expose arbitrary scores as if they
            # were scientific measurements.
            score = item.get("priority_score")

            if not isinstance(score, (int, float)):
                score = None
            else:
                score = max(
                    0,
                    min(
                        100,
                        int(score)
                    )
                )

            priority_actions.append({
                "problem_id": _string(
                    item.get("problem_id")
                ),
                "priority_score": score,
                "priority_reason": _string(
                    item.get("priority_reason")
                ),
                "confidence": confidence
            })

    # Only problems with usable evidence and non-unknown
    # confidence can become top priority actions.
    valid_problem_ids = {
        problem["id"]
        for problem in problems
        if problem["confidence"] != "unknown"
    }

    priority_actions = [
        action
        for action in priority_actions
        if action["problem_id"]
        in valid_problem_ids
    ]

    def string_list(value):
        if not isinstance(value, list):
            return []

        return [
            _string(item)
            for item in value
            if _string(item)
        ]

    score = raw.get("score")

    if not isinstance(score, (int, float)):
        score = 0

    score = max(
        0,
        min(
            100,
            int(score)
        )
    )

    return {
        "schema_version": "2.0",
        "quality": quality,
        "room": room,
        "summary": _string(
            raw.get("summary"),
            "No reliable summary was produced."
        ),
        "score": score,
        "observations": observations,
        "problems": problems,
        "risk_scanner": (
            raw.get("risk_scanner")
            if isinstance(
                raw.get("risk_scanner"),
                list
            )
            else []
        ),
        "keep": string_list(
            raw.get("keep")
        ),
        "replace": string_list(
            raw.get("replace")
        ),
        "upgrade": string_list(
            raw.get("upgrade")
        ),
        "priority_actions": priority_actions,
        "lifestyle_questions": string_list(
            raw.get("lifestyle_questions")
        )
    }
