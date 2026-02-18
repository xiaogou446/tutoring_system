import json
import os
import time
from http.client import RemoteDisconnected
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


class RelayLlmClient:
    """通过 OpenAI 兼容接口调用中转 LLM 服务。"""

    def __init__(
        self,
        config_path: str | None = None,
        base_url: str | None = None,
        api_key: str | None = None,
        model: str | None = None,
        timeout_seconds: int | None = None,
    ) -> None:
        default_config_path = Path(__file__).resolve().parents[1] / "llm_config.json"
        cfg = self._load_config_file(config_path or str(default_config_path))

        self.base_url = (
            base_url
            or str(cfg.get("base_url") or "").strip()
            or "https://api.openai.com/v1"
        )
        self.api_key = (api_key or str(cfg.get("api_key") or "").strip()).strip()
        self.model = (model or str(cfg.get("model") or "").strip()).strip()
        if timeout_seconds is not None:
            self.timeout_seconds = int(timeout_seconds)
        else:
            self.timeout_seconds = int(cfg.get("timeout_seconds") or 30)

    @staticmethod
    def _load_config_file(config_path: str) -> dict:
        path = Path(config_path)
        if not path.exists():
            return {}
        try:
            raw = path.read_text(encoding="utf-8").strip()
            if not raw:
                return {}
            payload = json.loads(raw)
            return payload if isinstance(payload, dict) else {}
        except Exception:  # noqa: BLE001
            return {}

    def is_configured(self) -> bool:
        return bool(self.api_key and self.model)

    def chat_completion(self, system_prompt: str, user_prompt: str) -> str:
        if not self.is_configured():
            raise RuntimeError("LLM 配置不完整，缺少 API key 或 model")

        endpoint = self.base_url.rstrip("/") + "/chat/completions"
        payload = {
            "model": self.model,
            "temperature": 0,
            "response_format": {"type": "json_object"},
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
        }
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.api_key}",
        }

        req = Request(endpoint, data=body, headers=headers, method="POST")
        last_error: Exception | None = None
        for attempt in range(1, 4):
            try:
                with urlopen(req, timeout=self.timeout_seconds) as response:
                    raw = response.read().decode("utf-8", "ignore")
                break
            except HTTPError as ex:
                detail = ex.read().decode("utf-8", "ignore") if ex.fp else str(ex)
                raise RuntimeError(f"LLM HTTP 错误: {ex.code} {detail}") from ex
            except (URLError, TimeoutError, RemoteDisconnected) as ex:
                last_error = ex
                if attempt >= 3:
                    raise RuntimeError(f"LLM 网络错误: {ex}") from ex
                time.sleep(1.2 * attempt)

        if last_error and not raw:
            raise RuntimeError(f"LLM 网络错误: {last_error}")

        data = json.loads(raw)
        choices = data.get("choices") or []
        if not choices:
            raise RuntimeError("LLM 响应缺少 choices")

        content = (
            choices[0].get("message", {}).get("content", "")
            if isinstance(choices[0], dict)
            else ""
        )
        if not content:
            raise RuntimeError("LLM 响应 content 为空")
        return content
