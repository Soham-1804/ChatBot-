#this is onky for offline(local network).
from flask import Flask, request, jsonify
from transformers import AutoModelForCausalLM, AutoTokenizer
import torch

app = Flask(__name__)

# Smaller and faster local model
MODEL_NAME = "microsoft/DialoGPT-small"  # ~350MB, replies instantly on CPU
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
model = AutoModelForCausalLM.from_pretrained(MODEL_NAME)

conversation_history = []

@app.route("/chat", methods=["POST"])
def chat():
    data = request.get_json()
    user_msg = data.get("message", "").strip()
    if not user_msg:
        return jsonify({"reply": "Please type something!"})

    # Encode user input and append to chat history
    new_input_ids = tokenizer.encode(user_msg + tokenizer.eos_token, return_tensors='pt')
    bot_input_ids = torch.cat(conversation_history + [new_input_ids], dim=-1) if conversation_history else new_input_ids

    # Generate response
    output = model.generate(
        bot_input_ids,
        max_length=1000,
        pad_token_id=tokenizer.eos_token_id,
        do_sample=True,
        top_k=50,
        top_p=0.95,
        temperature=0.7,
    )

    # Decode and clean reply
    reply = tokenizer.decode(output[:, bot_input_ids.shape[-1]:][0], skip_special_tokens=True)
    conversation_history.append(new_input_ids)
    conversation_history.append(output)

    return jsonify({"reply": reply.strip() or "Hmm, I’m not sure what to say."})

@app.route("/reset", methods=["POST"])
def reset():
    conversation_history.clear()
    return jsonify({"reply": "Chat reset successfully."})

if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000)
