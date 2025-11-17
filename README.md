## Samsa

> **~~Samsa is a high-performance Web framework~~ (goal)**

[ English | [中文](README_zh.md) ]

---

### 🌟 Core Philosophy

> **Embrace "Simple Synchronous", Abandon "Complex Asynchronous"**

Achieve near-asynchronous I/O performance without callback hell or complex reactive programming, while maintaining the readability and debugging experience of synchronous code.

---

### ⚡ Core Features

1. **Native Virtual Thread Support**  
   Leverages Java 21+ Project Loom capabilities to handle high-concurrency I/O with minimal overhead.

2. **Bypasses Traditional Servlet Containers**  
   Eliminates heavyweight containers like Tomcat and Jetty, significantly reducing startup overhead and memory footprint.

3. **Minimalist High-Performance Routing**  
   Radix Tree-based implementation with O(k) route matching complexity.

4. **Zero-Allocation Context Objects**  
   Creates no additional wrapper objects during request processing, minimizing GC pressure.

5. **Extreme Lightweight**  
   Perfectly supports lightweight deployment options like GraalVM Native Image.

6. **Native MessagePack Support**  
   Provides a binary serialization format 3-5x faster than JSON with 40%+ smaller payload size.
