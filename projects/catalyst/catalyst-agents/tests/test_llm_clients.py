"""
Unit tests for LLM client implementations.

These tests verify that the HTTP/API calls are formatted correctly and responses
are parsed properly. We mock the external HTTP/API dependencies (httpx.Client for
LM Studio, google.genai.Client for Gemini) to test our code's behavior without
requiring actual API keys or network connectivity.

What these tests verify:
- HTTP request format (URL, JSON payload, headers)
- API call parameters (model, contents, config)
- Response parsing (extracting SQL from API responses)
- Error handling (HTTP errors, API errors, connection errors)

What these tests do NOT verify (requires integration tests with real APIs):
- Actual API connectivity
- Real API key validation
- Actual API response format
- Network timeouts/retries
"""

from unittest.mock import Mock, patch

import httpx
import pytest
from google.genai import types

from src.llm_clients import GeminiClient, LMStudioClient


class TestLMStudioClient:
    """Test LM Studio client HTTP implementation."""

    def test_lmstudio_client_initialization(self):
        """Test that LMStudioClient initializes with correct parameters."""
        client = LMStudioClient("http://localhost:1234/v1", "test-model")
        assert client._base_url == "http://localhost:1234/v1"
        assert client._model == "test-model"

    @patch("httpx.Client")
    def test_lmstudio_generate_sql_makes_correct_http_request(self, mock_client_class):
        """Test that LMStudioClient makes HTTP request with correct format."""
        # Mock HTTP response
        mock_response = Mock()
        mock_response.json.return_value = {
            "choices": [{"message": {"content": "SELECT COUNT(*) FROM sample"}}]
        }
        mock_response.raise_for_status = Mock()

        mock_client_instance = Mock()
        mock_client_instance.__enter__ = Mock(return_value=mock_client_instance)
        mock_client_instance.__exit__ = Mock(return_value=False)
        mock_client_instance.post.return_value = mock_response
        mock_client_class.return_value = mock_client_instance

        client = LMStudioClient("http://localhost:1234/v1", "test-model")
        result = client.generate_sql("Schema:\ntest\n\nQuestion:\nHow many?")

        # Verify HTTP request was made with correct format
        mock_client_instance.post.assert_called_once()
        call_args = mock_client_instance.post.call_args
        assert call_args[0][0] == "http://localhost:1234/v1/chat/completions"

        request_json = call_args[1]["json"]
        assert request_json["model"] == "test-model"
        assert request_json["temperature"] == 0.1
        assert len(request_json["messages"]) == 2
        assert request_json["messages"][0]["role"] == "system"
        assert "SQL expert" in request_json["messages"][0]["content"]
        assert request_json["messages"][1]["role"] == "user"
        assert request_json["messages"][1]["content"] == "Schema:\ntest\n\nQuestion:\nHow many?"

        # Verify response parsing
        assert result == "SELECT COUNT(*) FROM sample"

    @patch("httpx.Client")
    def test_lmstudio_handles_http_errors(self, mock_client_class):
        """Test that LMStudioClient handles HTTP errors correctly."""
        mock_client_instance = Mock()
        mock_client_instance.__enter__ = Mock(return_value=mock_client_instance)
        mock_client_instance.__exit__ = Mock(return_value=False)
        mock_client_instance.post.side_effect = httpx.HTTPStatusError(
            "Server error", request=Mock(), response=Mock(status_code=500)
        )
        mock_client_class.return_value = mock_client_instance

        client = LMStudioClient("http://localhost:1234/v1", "test-model")
        with pytest.raises(httpx.HTTPStatusError):
            client.generate_sql("test prompt")

    @patch("httpx.Client")
    def test_lmstudio_handles_connection_errors(self, mock_client_class):
        """Test that LMStudioClient handles connection errors."""
        mock_client_instance = Mock()
        mock_client_instance.__enter__ = Mock(return_value=mock_client_instance)
        mock_client_instance.__exit__ = Mock(return_value=False)
        mock_client_instance.post.side_effect = httpx.ConnectError("Connection refused")
        mock_client_class.return_value = mock_client_instance

        client = LMStudioClient("http://localhost:1234/v1", "test-model")
        with pytest.raises(httpx.ConnectError):
            client.generate_sql("test prompt")

    @patch("httpx.Client")
    def test_lmstudio_parses_response_correctly(self, mock_client_class):
        """Test that LMStudioClient parses OpenAI-compatible response format."""
        mock_response = Mock()
        mock_response.json.return_value = {
            "choices": [{"message": {"content": "  SELECT * FROM test WHERE id = 1  \n"}}]
        }
        mock_response.raise_for_status = Mock()

        mock_client_instance = Mock()
        mock_client_instance.__enter__ = Mock(return_value=mock_client_instance)
        mock_client_instance.__exit__ = Mock(return_value=False)
        mock_client_instance.post.return_value = mock_response
        mock_client_class.return_value = mock_client_instance

        client = LMStudioClient("http://localhost:1234/v1", "test-model")
        result = client.generate_sql("test")

        # Verify whitespace is stripped
        assert result == "SELECT * FROM test WHERE id = 1"


class TestGeminiClient:
    """Test Gemini client API implementation."""

    def test_gemini_client_initialization(self):
        """Test that GeminiClient initializes with correct parameters."""
        with patch("google.genai.Client") as mock_client_class:
            mock_client_instance = Mock()
            mock_client_class.return_value = mock_client_instance

            client = GeminiClient("test-api-key", "gemini-pro")

            # Verify genai.Client was called with API key
            mock_client_class.assert_called_once_with(api_key="test-api-key")
            assert client._model == "gemini-pro"
            assert client._client == mock_client_instance

    @patch("google.genai.Client")
    def test_gemini_generate_sql_makes_correct_api_call(self, mock_client_class):
        """Test that GeminiClient makes API call with correct format."""
        # Mock Gemini API response
        mock_response = Mock()
        mock_response.text = "SELECT COUNT(*) FROM sample"

        mock_client_instance = Mock()
        mock_client_instance.models.generate_content.return_value = mock_response
        mock_client_class.return_value = mock_client_instance

        client = GeminiClient("test-api-key", "gemini-pro")
        result = client.generate_sql("Schema:\ntest\n\nQuestion:\nHow many?")

        # Verify API call was made with correct parameters
        mock_client_instance.models.generate_content.assert_called_once()
        call_kwargs = mock_client_instance.models.generate_content.call_args[1]

        assert call_kwargs["model"] == "gemini-pro"
        assert call_kwargs["contents"] == "Schema:\ntest\n\nQuestion:\nHow many?"
        assert isinstance(call_kwargs["config"], types.GenerateContentConfig)
        assert call_kwargs["config"].temperature == 0.1
        assert call_kwargs["config"].top_p == 0.95
        assert call_kwargs["config"].top_k == 40

        # Verify response parsing
        assert result == "SELECT COUNT(*) FROM sample"

    @patch("google.genai.Client")
    def test_gemini_handles_api_errors(self, mock_client_class):
        """Test that GeminiClient handles API errors correctly."""
        mock_client_instance = Mock()
        mock_client_instance.models.generate_content.side_effect = Exception("API error")
        mock_client_class.return_value = mock_client_instance

        client = GeminiClient("test-api-key", "gemini-pro")
        with pytest.raises(Exception, match="API error"):
            client.generate_sql("test prompt")

    @patch("google.genai.Client")
    def test_gemini_strips_whitespace_from_response(self, mock_client_class):
        """Test that GeminiClient strips whitespace from response."""
        mock_response = Mock()
        mock_response.text = "  SELECT * FROM test  \n"

        mock_client_instance = Mock()
        mock_client_instance.models.generate_content.return_value = mock_response
        mock_client_class.return_value = mock_client_instance

        client = GeminiClient("test-api-key", "gemini-pro")
        result = client.generate_sql("test")

        assert result == "SELECT * FROM test"
