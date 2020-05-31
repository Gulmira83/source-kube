from flask import Flask, jsonify
app = Flask(__name__)

@app.route('/')
def hello_world():
    return jsonify({"source-kube": "Welcome to Source Kube!!", "message": "Hello World"})


if __name__ == "__main__":
    app.run(host='0.0.0.0')