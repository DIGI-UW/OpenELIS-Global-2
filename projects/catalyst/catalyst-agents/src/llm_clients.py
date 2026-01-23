class LMStudioClient:
    def __init__(self, base_url: str, model: str) -> None:
        self._base_url = base_url
        self._model = model

    def generate_sql(self, prompt: str) -> str:
        # TODO: Replace with OpenAI-compatible call to LM Studio.
        return "SELECT 1"
