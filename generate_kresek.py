import wave, struct, random

def generate_white_noise(filename, duration_sec, sample_rate=44100):
    num_samples = int(duration_sec * sample_rate)
    with wave.open(filename, 'w') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        
        for _ in range(num_samples):
            # Generate random 16-bit integer
            value = random.randint(-32768, 32767)
            wav_file.writeframes(struct.pack('<h', value))

if __name__ == '__main__':
    generate_white_noise('assets/kresek.wav', 0.2)
