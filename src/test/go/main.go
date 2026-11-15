package main

import (
	"bufio"
	"errors"
	"fmt"
	"io"
	"net"
	"net/http"
	"strings"
	"syscall"
	"time"
)

func main() {
	s := "tcp"
	port := "127.0.0.1:8888"
	conn, err := net.DialTimeout(s, port, 3*time.Second)
	if err != nil {
		panic(err)
	}
	defer conn.Close()

	if err := conn.SetDeadline(time.Now().Add(3 * time.Second)); err != nil {
		panic(err)
	}
	first := "POST /split HTTP/1.1\r\nHost: localhost\r\nContent-Length: 5\r\n\r\nhe"
	if _, err := io.WriteString(conn, first); err != nil {
		panic(err)
	}
	time.Sleep(500 * time.Millisecond)
	if _, err := io.WriteString(conn, "llo"); err != nil {
		panic(err)
	}
	requests := "GET /first HTTP/1.1\r\nHost: localhost\r\n\r\n" +
		"GET /second HTTP/1.1\r\nHost: localhost\r\n\r\n"
	if _, err := io.WriteString(conn, requests); err != nil {
		panic(err)
	}
	reader := bufio.NewReader(conn)
	printResponse(reader, "POST /split\nhello")
	printResponse(reader, "GET /first")
	printResponse(reader, "GET /second")
	fmt.Println("-----test1 done-----")
	testOversize()
}
func testOversize() {
	s := "tcp"
	port := "127.0.0.1:8888"
	conn, err := net.DialTimeout(s, port, 3*time.Second)
	if err != nil {
		panic(err)
	}
	defer conn.Close()

	if err := conn.SetDeadline(time.Now().Add(3 * time.Second)); err != nil {
		panic(err)
	}
	body := strings.Repeat("x", 5000)
	request := fmt.Sprintf("POST /oversize HTTP/1.1\r\nHost: localhost\r\nContent-Length: %d\r\n\r\n%s", len(body), body)
	if _, err := io.WriteString(conn, request); err != nil {
		panic(err)
	}
	_, err = bufio.NewReader(conn).ReadByte()
	if !errors.Is(err, io.EOF) && !errors.Is(err, syscall.ECONNRESET) {
		panic(fmt.Sprintf("expected connection close, got %v", err))
	}
	fmt.Println("PASS: oversized request closed the connection")
}
func printResponse(reader *bufio.Reader, want string) {
	resp, err := http.ReadResponse(reader, nil)
	if err != nil {
		panic(err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)

	if err != nil {
		panic(err)
	}
	if resp.StatusCode != http.StatusOK || !strings.Contains(string(body), want) {
		panic(fmt.Sprintf("unexpected response: %s %q", resp.Status, body))
	}
	fmt.Printf("%s\n%s\n", resp.Status, body)

}
